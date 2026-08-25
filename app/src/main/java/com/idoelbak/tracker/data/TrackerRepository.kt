package com.idoelbak.tracker.data

import com.idoelbak.tracker.core.engine.DayBoundary
import com.idoelbak.tracker.core.engine.DueCalculator
import com.idoelbak.tracker.core.model.DueState
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.db.CompletionEntity
import com.idoelbak.tracker.data.db.DayRatingEntity
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.data.db.StreakStateEntity
import com.idoelbak.tracker.data.db.TrackerDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One dot in a habit row's week strip. */
enum class DayDot {
    /** Done. */
    HIT,

    /** Was due and was not done. */
    MISS,

    /** Today, still open. */
    TODAY,

    /** Still to come. */
    FUTURE,

    /** Not expected that day -- a rest day is not a miss. */
    IDLE
}

data class HabitRow(
    val id: Long,
    val name: String,
    val emoji: String?,
    val done: Boolean,
    val week: List<DayDot>,
    /** Only for weekly-quota habits: "1 of 3 this week". */
    val quota: String? = null
)

data class TodayUi(
    val date: LocalDate,
    val streak: Int = 0,
    val freezes: Int = 0,
    /** Required today. These are what the day's verdict is judged on. */
    val due: List<HabitRow> = emptyList(),
    /** Weekly-quota habits with slack left -- available today, not required. */
    val weekly: List<HabitRow> = emptyList(),
    val mood: Int? = null,
    val motivation: Int? = null
) {
    val doneCount: Int get() = due.count { it.done }
    val dueCount: Int get() = due.size
    val left: Int get() = dueCount - doneCount
}

/** A habit as it appears on the "all habits" page: every one defined, due today or not. */
data class DefinedHabit(
    val habit: HabitEntity,
    val state: DueState,
    val doneToday: Boolean,
    val doneThisWeek: Int
)

/**
 * The one place the DAOs meet the engine.
 *
 * Reads are flows, so the Today screen re-renders the moment a tick lands. Every date question goes
 * through [DayBoundary] and [DueCalculator] rather than being answered here.
 */
class TrackerRepository(
    private val db: TrackerDatabase,
    private val now: () -> LocalDateTime = LocalDateTime::now
) {

    // ponytail: fixed until Settings ships the week-start picker. Israel starts on Sunday.
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY

    fun today(): LocalDate = DayBoundary.trackingDate(now())

    /** Settles every day that finished while the app was closed. Cheap when there is nothing to do. */
    suspend fun settle(): Int = DayCloser(
        habitsOn = { date -> db.habits().activeOn(endOfDayMillis(date)) },
        completionsBetween = { from, to -> db.completions().between(from, to).map { it.habitId to it.date } },
        recordExists = { date -> db.dayRecords().on(date) != null },
        writeRecord = { db.dayRecords().close(it) },
        readState = { db.streakState().current() },
        writeState = { db.streakState().put(it) },
        weekStart = weekStart
    ).settleThrough(today())

    fun observeToday(date: LocalDate): Flow<TodayUi> {
        val from = DayBoundary.weekStartOf(date, weekStart)
        return combine(
            db.habits().observeActive(),
            db.completions().observeBetween(from, from.plusDays(6)),
            db.streakState().observe(),
            db.dayRatings().observeOn(date)
        ) { habits, ticks, streak, rating ->
            buildToday(date, from, habits, ticks, streak, rating, weekStart)
        }
    }

    /** Everything defined, whatever today happens to be -- including habits resting today. */
    fun observeDefined(date: LocalDate): Flow<List<DefinedHabit>> {
        val from = DayBoundary.weekStartOf(date, weekStart)
        return combine(
            db.habits().observeAll(),
            db.completions().observeBetween(from, from.plusDays(6))
        ) { habits, ticks ->
            habits.map { habit ->
                val mine = ticks.filter { it.habitId == habit.id }
                DefinedHabit(
                    habit = habit,
                    state = DueCalculator.dueState(
                        habit.schedule, date, weekStart, mine.count { it.date < date }
                    ),
                    doneToday = mine.any { it.date == date },
                    doneThisWeek = mine.size
                )
            }
        }
    }

    suspend fun toggle(habitId: Long, date: LocalDate) {
        if (db.completions().isDone(habitId, date)) {
            db.completions().untick(habitId, date)
        } else {
            db.completions().tick(CompletionEntity(habitId, date, millis()))
        }
    }

    suspend fun rate(date: LocalDate, mood: Int?, motivation: Int?) =
        db.dayRatings().put(DayRatingEntity(date, mood, motivation, millis()))

    /** Insert when [id] is null, otherwise edit in place. Editing never touches past ticks. */
    suspend fun saveHabit(
        id: Long?,
        name: String,
        emoji: String?,
        schedule: Schedule
    ) {
        val habits = db.habits()
        val existing = id?.let { habits.byId(it) }
        if (existing == null) {
            habits.insert(
                HabitEntity(
                    name = name.trim(),
                    emoji = emoji,
                    scheduleType = schedule.type,
                    timesPerWeek = schedule.timesPerWeek,
                    weekdayMask = schedule.weekdayMask,
                    orderIndex = habits.nextOrderIndex(),
                    createdAt = millis()
                )
            )
        } else {
            habits.update(
                existing.copy(
                    name = name.trim(),
                    emoji = emoji,
                    scheduleType = schedule.type,
                    timesPerWeek = schedule.timesPerWeek,
                    weekdayMask = schedule.weekdayMask
                )
            )
        }
    }

    suspend fun archive(id: Long) = db.habits().archive(id, millis())

    suspend fun unarchive(id: Long) = db.habits().unarchive(id)

    private fun millis(): Long = now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** A tracked day ends at the rollover, not at midnight. */
    private fun endOfDayMillis(date: LocalDate): Long =
        DayBoundary.endOf(date).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * Turns a week of raw rows into what Today shows.
 *
 * The rule that matters: a habit not expected today is in neither list. It is not greyed out at the
 * bottom of the day -- it simply is not part of today, and comes back on the day it is due.
 */
internal fun buildToday(
    date: LocalDate,
    weekFrom: LocalDate,
    habits: List<HabitEntity>,
    ticks: List<CompletionEntity>,
    streak: StreakStateEntity?,
    rating: DayRatingEntity?,
    weekStart: DayOfWeek
): TodayUi {
    val due = mutableListOf<HabitRow>()
    val weekly = mutableListOf<HabitRow>()

    for (habit in habits) {
        val mine = ticks.filter { it.habitId == habit.id }
        val doneToday = mine.any { it.date == date }
        val state = DueCalculator.dueState(
            habit.schedule, date, weekStart, mine.count { it.date < date }
        )

        val row = HabitRow(
            id = habit.id,
            name = habit.name,
            emoji = habit.emoji,
            done = doneToday,
            week = weekStrip(habit, date, weekFrom, mine, weekStart),
            quota = if (habit.scheduleType == ScheduleType.TIMES_PER_WEEK) {
                "${mine.size} of ${habit.timesPerWeek} this week"
            } else {
                null
            }
        )

        when {
            state == DueState.DUE -> due += row
            // Available today, or already ticked as a bonus. Anything else is resting: it stays
            // off Today and lives on the Habits page until the day it comes round again.
            state == DueState.OPEN || doneToday -> weekly += row
        }
    }

    return TodayUi(
        date = date,
        streak = streak?.currentStreak ?: 0,
        freezes = streak?.freezes ?: 0,
        due = due,
        weekly = weekly,
        mood = rating?.mood,
        motivation = rating?.motivation
    )
}

private fun weekStrip(
    habit: HabitEntity,
    date: LocalDate,
    weekFrom: LocalDate,
    mine: List<CompletionEntity>,
    weekStart: DayOfWeek
): List<DayDot> = (0..6).map { offset ->
    val day = weekFrom.plusDays(offset.toLong())
    val hit = mine.any { it.date == day }
    when {
        day.isAfter(date) -> DayDot.FUTURE
        hit -> DayDot.HIT
        day == date -> DayDot.TODAY
        DueCalculator.dueState(
            habit.schedule, day, weekStart, mine.count { it.date < day }
        ) == DueState.DUE -> DayDot.MISS
        else -> DayDot.IDLE
    }
}
