package com.idoelbak.tracker.core.model

import java.time.DayOfWeek

enum class ScheduleType {
    /** Due every single day. */
    DAILY,

    /** A weekly quota on any days you like -- the spreadsheet's "Workout 3". */
    TIMES_PER_WEEK,

    /** Due only on fixed weekdays -- gym days. */
    SPECIFIC_DAYS
}

/**
 * How often a habit is due.
 *
 * [weekdayMask] uses ISO weekday numbering (Monday = 1 .. Sunday = 7) with bit `value - 1` set,
 * so it stays correct no matter which day the user's week starts on.
 */
data class Schedule(
    val type: ScheduleType,
    val timesPerWeek: Int = 0,
    val weekdayMask: Int = 0
) {
    fun includes(day: DayOfWeek): Boolean = weekdayMask and day.bit() != 0

    /** How many times this schedule expects in a full week -- the spreadsheet's "weekly goal" column. */
    val weeklyGoal: Int
        get() = when (type) {
            ScheduleType.DAILY -> 7
            ScheduleType.TIMES_PER_WEEK -> timesPerWeek
            ScheduleType.SPECIFIC_DAYS -> Integer.bitCount(weekdayMask)
        }

    companion object {
        fun daily() = Schedule(ScheduleType.DAILY)

        fun timesPerWeek(times: Int): Schedule {
            require(times in 1..7) { "timesPerWeek must be 1..7, was $times" }
            return Schedule(ScheduleType.TIMES_PER_WEEK, timesPerWeek = times)
        }

        fun onDays(vararg days: DayOfWeek): Schedule {
            require(days.isNotEmpty()) { "SPECIFIC_DAYS needs at least one day" }
            return Schedule(ScheduleType.SPECIFIC_DAYS, weekdayMask = days.fold(0) { acc, d -> acc or d.bit() })
        }
    }
}

fun DayOfWeek.bit(): Int = 1 shl (value - 1)

/**
 * Whether a habit needs doing today.
 *
 * [DUE] counts toward the day's streak verdict. [OPEN] does not -- it is a weekly-quota habit that
 * still has slack, so missing it today costs nothing yet.
 */
enum class DueState {
    /** Must be done today, or the day is a miss. */
    DUE,

    /** Can be done today and counts toward the weekly quota, but is not required yet. */
    OPEN,

    /** Not expected today at all. */
    NOT_DUE
}
