package com.idoelbak.tracker.core.engine

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The spans the stats screen reads over.
 *
 * The grid deliberately shows the last four *weeks*, not the last calendar month. Weeks keep the
 * columns aligned to weekdays, which is the whole point of the grid -- you can see straight down a
 * column that Saturdays are where a habit dies. A calendar month starts on an arbitrary weekday and
 * ruins that.
 */
object StatsWindow {

    const val GRID_WEEKS = 4

    /** Inclusive range covering [weeks] whole weeks, ending with the week containing [today]. */
    fun lastWeeks(today: LocalDate, weekStart: DayOfWeek, weeks: Int = GRID_WEEKS): ClosedRange<LocalDate> {
        require(weeks >= 1) { "weeks must be at least 1" }
        val currentWeekStart = DayBoundary.weekStartOf(today, weekStart)
        val from = currentWeekStart.minusWeeks((weeks - 1).toLong())
        return from..currentWeekStart.plusDays(6)
    }

    /** The grid, one list per week, each exactly seven days -- including days still in the future. */
    fun gridWeeks(today: LocalDate, weekStart: DayOfWeek, weeks: Int = GRID_WEEKS): List<List<LocalDate>> {
        val range = lastWeeks(today, weekStart, weeks)
        return (0 until weeks).map { w ->
            val rowStart = range.start.plusWeeks(w.toLong())
            (0..6).map { rowStart.plusDays(it.toLong()) }
        }
    }

    /** The seven weekday headings, ordered from the user's chosen week start. */
    fun weekdayOrder(weekStart: DayOfWeek): List<DayOfWeek> =
        (0..6).map { DayOfWeek.of(((weekStart.value - 1 + it) % 7) + 1) }

    /** Inclusive range for a rolling window ending today, used by the heatmaps. */
    fun lastDays(today: LocalDate, days: Int): ClosedRange<LocalDate> {
        require(days >= 1) { "days must be at least 1" }
        return today.minusDays((days - 1).toLong())..today
    }
}
