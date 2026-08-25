package com.idoelbak.tracker.data

import com.idoelbak.tracker.core.engine.DayBoundary
import com.idoelbak.tracker.core.engine.DayVerdict
import com.idoelbak.tracker.core.engine.DueCalculator
import com.idoelbak.tracker.core.engine.HabitStrength
import com.idoelbak.tracker.core.engine.MoodFinding
import com.idoelbak.tracker.core.engine.MoodInsights
import com.idoelbak.tracker.core.engine.RatedDay
import com.idoelbak.tracker.core.engine.StatsWindow
import com.idoelbak.tracker.core.model.DueState
import com.idoelbak.tracker.data.db.CompletionEntity
import com.idoelbak.tracker.data.db.DayRatingEntity
import com.idoelbak.tracker.data.db.DayRecordEntity
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.data.db.StreakStateEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class StatsPeriod(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All");

    /** Where this period starts, counting back from [today]. */
    fun from(today: LocalDate, weekStart: DayOfWeek): LocalDate = when (this) {
        WEEK -> DayBoundary.weekStartOf(today, weekStart)
        MONTH -> today.minusDays(29)
        YEAR -> today.minusDays(364)
        ALL -> LocalDate.ofEpochDay(0)
    }
}

data class StrengthRow(val id: Long, val name: String, val emoji: String?, val percent: Int)

/** One square of the four-week grid. */
data class HeatCell(val date: LocalDate, val level: Int, val future: Boolean)

data class WeekdayBar(val day: DayOfWeek, val percent: Int?)

data class TrendPoint(val weekStart: LocalDate, val percent: Int)

data class StatsUi(
    val period: StatsPeriod = StatsPeriod.MONTH,
    /** Null until at least one day has closed -- an empty app should say nothing, not "0%". */
    val completionPercent: Int? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val perfectDays: Int = 0,
    val daysTracked: Int = 0,
    val completions: Int = 0,
    val strength: List<StrengthRow> = emptyList(),
    val grid: List<List<HeatCell>> = emptyList(),
    val weekdays: List<WeekdayBar> = emptyList(),
    /** "Saturday is your weakest day by 8 points." Null when the difference is not worth saying. */
    val weakestDayNote: String? = null,
    val trend: List<TrendPoint> = emptyList(),
    val mood: MoodFinding? = null,
    val motivation: MoodFinding? = null
) {
    val hasHistory: Boolean get() = daysTracked > 0
}

/** Weeks of trend to draw, and the window habit strength is computed over. */
private const val TREND_WEEKS = 8
private const val STRENGTH_DAYS = 90

/** Below this many points a weak weekday is noise rather than a finding. */
private const val MIN_WEEKDAY_GAP = 5

/**
 * Everything the stats screen shows, from settled days only.
 *
 * Today is deliberately absent: it has not closed, so counting it would drag every percentage down
 * all morning and up again all evening.
 */
internal fun buildStats(
    today: LocalDate,
    weekStart: DayOfWeek,
    period: StatsPeriod,
    records: List<DayRecordEntity>,
    ratings: List<DayRatingEntity>,
    habits: List<HabitEntity>,
    ticks: List<CompletionEntity>,
    streak: StreakStateEntity?
): StatsUi {
    val inPeriod = records.filter { !it.date.isBefore(period.from(today, weekStart)) }
    val counted = inPeriod.filter { it.dueCount > 0 }

    val due = counted.sumOf { it.dueCount }
    val done = counted.sumOf { it.doneCount }

    return StatsUi(
        period = period,
        completionPercent = if (due == 0) null else (done * 100) / due,
        currentStreak = streak?.currentStreak ?: 0,
        bestStreak = streak?.bestStreak ?: 0,
        perfectDays = inPeriod.count { it.verdict == DayVerdict.PERFECT },
        daysTracked = counted.size,
        completions = done,
        strength = strengthRows(today, weekStart, habits, ticks),
        grid = heatGrid(today, weekStart, records),
        weekdays = weekdayBars(counted, weekStart),
        weakestDayNote = weakestDayNote(weekdayBars(counted, weekStart)),
        trend = trendPoints(today, weekStart, records),
        mood = MoodInsights.compare(ratedDays(inPeriod, ratings) { it.mood }),
        motivation = MoodInsights.compare(ratedDays(inPeriod, ratings) { it.motivation })
    )
}

/**
 * An exponential moving average per habit, over the days it was actually expected. Days it was not
 * due are skipped rather than counted as misses, so a rest day cannot dent a gym habit.
 */
private fun strengthRows(
    today: LocalDate,
    weekStart: DayOfWeek,
    habits: List<HabitEntity>,
    ticks: List<CompletionEntity>
): List<StrengthRow> {
    val first = today.minusDays((STRENGTH_DAYS - 1).toLong())
    return habits.map { habit ->
        val mine = ticks.filter { it.habitId == habit.id }
        val born = habit.createdAt.toLocalDate()
        val outcomes = mutableListOf<Boolean>()

        var day = if (born.isAfter(first)) born else first
        while (day.isBefore(today)) {
            val earlier = mine.count {
                it.date < day && !it.date.isBefore(DayBoundary.weekStartOf(day, weekStart))
            }
            if (DueCalculator.dueState(habit.schedule, day, weekStart, earlier) == DueState.DUE) {
                outcomes += mine.any { it.date == day }
            }
            day = day.plusDays(1)
        }

        StrengthRow(habit.id, habit.name, habit.emoji, HabitStrength.percent(outcomes))
    }.sortedByDescending { it.percent }
}

/** Four weeks, weekday columns aligned, so you can read straight down a column. */
private fun heatGrid(
    today: LocalDate,
    weekStart: DayOfWeek,
    records: List<DayRecordEntity>
): List<List<HeatCell>> = StatsWindow.gridWeeks(today, weekStart).map { week ->
    week.map { date ->
        val record = records.firstOrNull { it.date == date }
        HeatCell(
            date = date,
            level = when {
                record == null || record.dueCount == 0 -> 0
                else -> {
                    val fraction = record.doneCount.toDouble() / record.dueCount
                    if (fraction <= 0.0) 0 else (fraction * 4).roundToInt().coerceIn(1, 4)
                }
            },
            future = date.isAfter(today)
        )
    }
}

private fun weekdayBars(records: List<DayRecordEntity>, weekStart: DayOfWeek): List<WeekdayBar> =
    StatsWindow.weekdayOrder(weekStart).map { day ->
        val onThatDay = records.filter { it.date.dayOfWeek == day }
        val due = onThatDay.sumOf { it.dueCount }
        WeekdayBar(day, if (due == 0) null else (onThatDay.sumOf { it.doneCount } * 100) / due)
    }

/** The most actionable sentence on the screen -- but only when the gap is real. */
private fun weakestDayNote(bars: List<WeekdayBar>): String? {
    val known = bars.filter { it.percent != null }
    if (known.size < 4) return null
    val worst = known.minByOrNull { it.percent!! } ?: return null
    val others = known.filter { it.day != worst.day }
    if (others.isEmpty()) return null
    val average = others.sumOf { it.percent!! } / others.size
    val gap = average - worst.percent!!
    if (gap < MIN_WEEKDAY_GAP) return null
    val name = worst.day.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$name is your weakest day by $gap points."
}

private fun trendPoints(
    today: LocalDate,
    weekStart: DayOfWeek,
    records: List<DayRecordEntity>
): List<TrendPoint> {
    val currentWeek = DayBoundary.weekStartOf(today, weekStart)
    return (TREND_WEEKS - 1 downTo 0).mapNotNull { back ->
        val from = currentWeek.minusWeeks(back.toLong())
        val week = records.filter { it.date >= from && it.date <= from.plusDays(6) && it.dueCount > 0 }
        val due = week.sumOf { it.dueCount }
        if (due == 0) null else TrendPoint(from, (week.sumOf { it.doneCount } * 100) / due)
    }
}

private fun ratedDays(
    records: List<DayRecordEntity>,
    ratings: List<DayRatingEntity>,
    pick: (DayRatingEntity) -> Int?
): List<RatedDay> = records.map { record ->
    RatedDay(
        rating = ratings.firstOrNull { it.date == record.date }?.let(pick),
        dueCount = record.dueCount,
        doneCount = record.doneCount
    )
}

private fun Long.toLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
