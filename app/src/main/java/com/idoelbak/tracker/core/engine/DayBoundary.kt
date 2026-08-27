package com.idoelbak.tracker.core.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Where one tracked day ends and the next begins.
 *
 * The day does not roll over at midnight. With the default 03:00 boundary, ticking something off at
 * 01:30 still counts for the evening you are actually still in.
 */
object DayBoundary {

    const val DEFAULT_ROLLOVER_HOUR = 3

    /** The day a moment belongs to. */
    fun trackingDate(now: LocalDateTime, rolloverHour: Int = DEFAULT_ROLLOVER_HOUR): LocalDate =
        if (now.hour < rolloverHour) now.toLocalDate().minusDays(1) else now.toLocalDate()

    /** The moment [date] stops accepting ticks. */
    fun endOf(date: LocalDate, rolloverHour: Int = DEFAULT_ROLLOVER_HOUR): LocalDateTime =
        date.plusDays(1).atStartOfDay().plusHours(rolloverHour.toLong())

    /** First day of the week containing [date], given the user's chosen week start. */
    fun weekStartOf(date: LocalDate, weekStart: DayOfWeek): LocalDate {
        val shift = Math.floorMod(date.dayOfWeek.value - weekStart.value, 7)
        return date.minusDays(shift.toLong())
    }

    /** Last day of the week containing [date]. */
    fun weekEndOf(date: LocalDate, weekStart: DayOfWeek): LocalDate =
        weekStartOf(date, weekStart).plusDays(6)

    /**
     * Days left in the week including [date] itself.
     *
     * This is the number that makes a weekly-quota habit go from optional to mandatory: once the
     * sessions you still owe reach the days you have left, today stops being a choice.
     */
    fun daysRemainingInWeek(date: LocalDate, weekStart: DayOfWeek): Int =
        7 - Math.floorMod(date.dayOfWeek.value - weekStart.value, 7)
}
