package com.idoelbak.tracker.core.engine

import kotlin.math.abs
import kotlin.math.roundToInt

/** One day's self-rating paired with what actually got done that day. */
data class RatedDay(
    /** 1..10, or null if the day was not rated. */
    val rating: Int?,
    val dueCount: Int,
    val doneCount: Int
) {
    val completionRate: Double? = if (dueCount > 0) doneCount.toDouble() / dueCount else null
}

/**
 * What a low day costs you.
 *
 * The finding is deliberately a difference of two averages rather than a correlation coefficient.
 * "You finish 30% less on low-motivation days" is something you can act on; "r = -0.42" is not.
 */
data class MoodFinding(
    val lowDays: Int,
    val highDays: Int,
    val lowCompletionPercent: Int,
    val highCompletionPercent: Int
) {
    /** Percentage points lost on low days. Negative means low days were actually better. */
    val gapPercentagePoints: Int get() = highCompletionPercent - lowCompletionPercent

    /** Relative drop, which is the figure worth showing: "you finish 30% less". */
    val relativeDropPercent: Int
        get() = if (highCompletionPercent == 0) 0
        else ((gapPercentagePoints.toDouble() / highCompletionPercent) * 100).roundToInt()
}

object MoodInsights {

    /** At or below this counts as a low day. */
    const val LOW_MAX = 4

    /** At or above this counts as a high day. */
    const val HIGH_MIN = 7

    /** Fewer rated days than this on either side and the comparison is not worth showing. */
    const val MIN_DAYS_PER_SIDE = 4

    /** Below this many percentage points the difference is noise, not a finding. */
    const val MIN_REPORTABLE_GAP = 8

    /**
     * @return null when there is not enough rated history, or when the difference is too small to
     *   be worth a sentence. Saying nothing beats manufacturing an insight.
     */
    fun compare(days: List<RatedDay>): MoodFinding? {
        val usable = days.filter { it.rating != null && it.completionRate != null }
        val low = usable.filter { it.rating!! <= LOW_MAX }
        val high = usable.filter { it.rating!! >= HIGH_MIN }

        if (low.size < MIN_DAYS_PER_SIDE || high.size < MIN_DAYS_PER_SIDE) return null

        val lowPct = (low.map { it.completionRate!! }.average() * 100).roundToInt()
        val highPct = (high.map { it.completionRate!! }.average() * 100).roundToInt()

        if (abs(highPct - lowPct) < MIN_REPORTABLE_GAP) return null

        return MoodFinding(
            lowDays = low.size,
            highDays = high.size,
            lowCompletionPercent = lowPct,
            highCompletionPercent = highPct
        )
    }

    /** Mean rating over the rated days, or null if none were rated. */
    fun averageRating(days: List<RatedDay>): Double? =
        days.mapNotNull { it.rating }.takeIf { it.isNotEmpty() }?.average()
}
