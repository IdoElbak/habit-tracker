package com.idoelbak.tracker.data

import com.idoelbak.tracker.core.engine.DayBoundary
import com.idoelbak.tracker.core.engine.DueCalculator
import com.idoelbak.tracker.core.engine.StreakEngine
import com.idoelbak.tracker.core.engine.StreakState
import com.idoelbak.tracker.core.model.DueState
import com.idoelbak.tracker.data.db.DayRecordEntity
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.data.db.StreakStateEntity
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Settles every day that has finished since the app last looked.
 *
 * The app is not running at 03:00, so days cannot close themselves on a timer alone. This runs on
 * launch, on the periodic worker and before any widget refresh, and walks forward one day at a time
 * from the last settled day to yesterday.
 *
 * Two invariants matter and are tested:
 *  - **Today is never closed.** There is still time left in it.
 *  - **A day already in `day_records` is never revisited.** Its verdict was computed under the rules
 *    in force at the time and stays that way forever.
 */
class DayCloser(
    private val habitsOn: suspend (LocalDate) -> List<HabitEntity>,
    private val completionsBetween: suspend (LocalDate, LocalDate) -> List<Pair<Long, LocalDate>>,
    private val recordExists: suspend (LocalDate) -> Boolean,
    private val writeRecord: suspend (DayRecordEntity) -> Unit,
    private val readState: suspend () -> StreakStateEntity?,
    private val writeState: suspend (StreakStateEntity) -> Unit,
    private val weekStart: DayOfWeek,
    private val now: () -> Long = System::currentTimeMillis
) {

    suspend fun settleThrough(today: LocalDate): Int {
        val stored = readState() ?: StreakStateEntity()
        var state = StreakState(
            currentStreak = stored.currentStreak,
            bestStreak = stored.bestStreak,
            perfectDays = stored.perfectDays,
            freezes = stored.freezes,
            perfectRun = stored.perfectRun
        )

        val firstOpen = stored.lastClosedDate?.plusDays(1) ?: earliestInterestingDay(today) ?: return 0
        var date = firstOpen
        var closed = 0

        // Yesterday is the last day that can be settled; today is still being lived.
        while (date.isBefore(today)) {
            if (!recordExists(date)) {
                val (due, done) = countsFor(date)
                val outcome = StreakEngine.closeDay(state, due, done)
                writeRecord(
                    DayRecordEntity(
                        date = date,
                        dueCount = due,
                        doneCount = done,
                        verdict = outcome.verdict,
                        allowanceApplied = outcome.allowanceApplied,
                        freezeSpent = outcome.freezeSpent,
                        freezeEarned = outcome.freezeEarned,
                        streakAfter = outcome.state.currentStreak,
                        closedAt = now()
                    )
                )
                state = outcome.state
                closed++
            }
            date = date.plusDays(1)
        }

        writeState(
            StreakStateEntity(
                currentStreak = state.currentStreak,
                bestStreak = state.bestStreak,
                perfectDays = state.perfectDays,
                freezes = state.freezes,
                perfectRun = state.perfectRun,
                lastClosedDate = today.minusDays(1)
            )
        )
        return closed
    }

    /** How many habits were due on [date], and how many of those were actually ticked. */
    suspend fun countsFor(date: LocalDate): Pair<Int, Int> {
        val habits = habitsOn(date)
        if (habits.isEmpty()) return 0 to 0

        val weekFrom = DayBoundary.weekStartOf(date, weekStart)
        val ticks = completionsBetween(weekFrom, date)
        val doneOnDate = ticks.filter { it.second == date }.map { it.first }.toSet()

        var due = 0
        var done = 0
        for (habit in habits) {
            val earlierThisWeek = ticks.count { it.first == habit.id && it.second < date }
            val state = DueCalculator.dueState(habit.schedule, date, weekStart, earlierThisWeek)
            if (state == DueState.DUE) {
                due++
                if (habit.id in doneOnDate) done++
            }
        }
        return due to done
    }

    /**
     * With no history at all there is nothing to settle. Tracking starts the day the first habit is
     * created, so the app cannot invent a wall of missed days for time before it existed.
     */
    private suspend fun earliestInterestingDay(today: LocalDate): LocalDate? {
        val habits = habitsOn(today)
        if (habits.isEmpty()) return null
        return habits.minOf { java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    }
}
