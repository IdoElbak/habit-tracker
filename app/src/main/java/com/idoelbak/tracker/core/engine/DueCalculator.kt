package com.idoelbak.tracker.core.engine

import com.idoelbak.tracker.core.model.DueState
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.core.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Decides whether a habit is required today, merely available, or not expected at all.
 *
 * The interesting case is [ScheduleType.TIMES_PER_WEEK]. A "3 times per week" habit drifts along as
 * [DueState.OPEN] while there is still slack, and escalates to [DueState.DUE] as soon as the sessions
 * still owed reach the days still left -- and stays there if it falls further behind, which back-fill
 * on the Week grid or a raised quota can both cause. That escalation is what the end-of-week
 * catch-up notification fires on, and it is why a gym habit never reads as "missed" on a rest day.
 */
object DueCalculator {

    /**
     * @param completionsEarlierInWeek ticks already banked this week BEFORE [date] -- today's own tick
     *   is deliberately excluded so the answer does not change the instant the user taps.
     */
    fun dueState(
        schedule: Schedule,
        date: LocalDate,
        weekStart: DayOfWeek,
        completionsEarlierInWeek: Int = 0
    ): DueState = when (schedule.type) {
        ScheduleType.DAILY -> DueState.DUE

        ScheduleType.SPECIFIC_DAYS ->
            if (schedule.includes(date.dayOfWeek)) DueState.DUE else DueState.NOT_DUE

        ScheduleType.TIMES_PER_WEEK -> {
            val stillOwed = schedule.timesPerWeek - completionsEarlierInWeek
            val daysLeft = DayBoundary.daysRemainingInWeek(date, weekStart)
            when {
                stillOwed <= 0 -> DueState.NOT_DUE        // quota already met this week
                stillOwed >= daysLeft -> DueState.DUE     // no slack left; today is mandatory
                else -> DueState.OPEN
            }
        }
    }

    /**
     * True when a weekly-quota habit has just become mandatory -- i.e. it was optional yesterday and
     * is required today. This is the trigger for "2 gym sessions, 2 days left".
     */
    fun justEscalated(
        schedule: Schedule,
        date: LocalDate,
        weekStart: DayOfWeek,
        completionsEarlierInWeek: Int
    ): Boolean {
        if (schedule.type != ScheduleType.TIMES_PER_WEEK) return false
        if (dueState(schedule, date, weekStart, completionsEarlierInWeek) != DueState.DUE) return false
        val yesterday = date.minusDays(1)
        // Only meaningful inside the same week; the first day of a week cannot have escalated.
        if (DayBoundary.weekStartOf(yesterday, weekStart) != DayBoundary.weekStartOf(date, weekStart)) return false
        return dueState(schedule, yesterday, weekStart, completionsEarlierInWeek) == DueState.OPEN
    }

    /** Sessions still owed on the weekly quota, floored at zero. */
    fun stillOwedThisWeek(schedule: Schedule, completionsThisWeek: Int): Int =
        when (schedule.type) {
            ScheduleType.TIMES_PER_WEEK -> (schedule.timesPerWeek - completionsThisWeek).coerceAtLeast(0)
            else -> (schedule.weeklyGoal - completionsThisWeek).coerceAtLeast(0)
        }
}
