package com.idoelbak.tracker.core.engine

import kotlin.math.pow

/**
 * Loop Habit Tracker's idea, and the most useful number on the stats screen.
 *
 * A streak is binary and brittle: one miss and months of work reads as zero. Strength is an
 * exponential moving average instead -- a miss dents it, practice rebuilds it, and nothing you do in
 * a single day can wipe it out. That matters most right after a streak breaks, which is exactly when
 * people quit.
 */
object HabitStrength {

    /** Days of practice for a habit's score to close half the gap to its target. */
    const val HALF_LIFE_DAYS = 13.0

    private val ALPHA = 1.0 - 0.5.pow(1.0 / HALF_LIFE_DAYS)

    /**
     * @param outcomes chronological, one entry per day the habit was actually expected --
     *   days it was not due are skipped entirely rather than counted as misses.
     * @return 0.0 .. 1.0
     */
    fun compute(outcomes: List<Boolean>): Double {
        var score = 0.0
        for (done in outcomes) {
            score += ALPHA * ((if (done) 1.0 else 0.0) - score)
        }
        return score.coerceIn(0.0, 1.0)
    }

    /** Whole-percent form, for display. */
    fun percent(outcomes: List<Boolean>): Int = Math.round(compute(outcomes) * 100).toInt()

    /** Score after one more day, without recomputing history. */
    fun step(current: Double, done: Boolean): Double =
        (current + ALPHA * ((if (done) 1.0 else 0.0) - current)).coerceIn(0.0, 1.0)
}
