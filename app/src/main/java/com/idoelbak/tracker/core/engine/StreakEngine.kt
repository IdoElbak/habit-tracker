package com.idoelbak.tracker.core.engine

/**
 * What a finished day turned out to be. Written once, when the day closes, and never recomputed --
 * that is what guarantees a later rule change cannot retroactively damage an existing streak.
 */
enum class DayVerdict {
    /** Everything due was done. */
    PERFECT,

    /** Within the allowance -- one miss on a day with enough due. */
    COMPLETE,

    /** Failed, but a freeze absorbed it. The streak survives at its current length. */
    FROZEN,

    /** Failed with no freeze left. */
    BROKEN,

    /** Nothing was due. Neither builds nor breaks anything. */
    EMPTY
}

data class StreakState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val perfectDays: Int = 0,
    val freezes: Int = StreakEngine.STARTING_FREEZES,
    /**
     * Days that counted, banked since the last freeze was earned -- 7 of them restore one freeze.
     * A day saved by the allowance counts, and spending a freeze does not wipe the progress:
     * a bad day costs you the freeze, not the seven days of work behind the next one.
     * Only actually breaking the streak resets it.
     */
    val cleanDays: Int = 0
)

data class DayOutcome(
    val verdict: DayVerdict,
    val state: StreakState,
    val allowanceApplied: Int,
    val freezeSpent: Boolean,
    val freezeEarned: Boolean
)

/**
 * The rules that decide whether a day counted.
 *
 * Two safety nets, deliberately different in kind:
 *
 *  - **The allowance** is routine slack: miss one thing and the day still counts, but only on days
 *    with enough due for that to be a small fraction. On a two-habit day, missing one is half of it,
 *    so the allowance does not apply and everything is required.
 *  - **A freeze** is for a genuinely bad day. It preserves the streak at its current length rather
 *    than extending it -- no work was done, so no day is gained. Freezes are scarce: two at most,
 *    and one comes back only after seven days that counted.
 */
object StreakEngine {

    const val MAX_FREEZES = 2
    const val STARTING_FREEZES = 2
    const val CLEAN_DAYS_PER_FREEZE = 7

    /** Below this many due, a single miss is too large a share of the day to forgive. */
    const val ALLOWANCE_MIN_DUE = 4

    fun allowanceFor(dueCount: Int): Int = if (dueCount >= ALLOWANCE_MIN_DUE) 1 else 0

    fun closeDay(previous: StreakState, dueCount: Int, doneCount: Int): DayOutcome {
        require(dueCount >= 0) { "dueCount must not be negative" }
        require(doneCount in 0..dueCount) { "doneCount $doneCount outside 0..$dueCount" }

        if (dueCount == 0) {
            return DayOutcome(DayVerdict.EMPTY, previous, allowanceApplied = 0, freezeSpent = false, freezeEarned = false)
        }

        val allowance = allowanceFor(dueCount)
        val missed = dueCount - doneCount

        return when {
            missed == 0 -> qualified(previous, DayVerdict.PERFECT, allowance)
            missed <= allowance -> qualified(previous, DayVerdict.COMPLETE, allowance)
            previous.freezes > 0 -> frozen(previous, allowance)
            else -> broken(previous, allowance)
        }
    }

    /** The day counted: extend the streak, and bank progress toward the next freeze. */
    private fun qualified(previous: StreakState, verdict: DayVerdict, allowance: Int): DayOutcome {
        val streak = previous.currentStreak + 1
        var freezes = previous.freezes
        var cleanDays = previous.cleanDays + 1
        var earned = false

        if (cleanDays >= CLEAN_DAYS_PER_FREEZE && freezes < MAX_FREEZES) {
            freezes += 1
            cleanDays = 0
            earned = true
        } else if (cleanDays >= CLEAN_DAYS_PER_FREEZE) {
            // Already holding the maximum; roll the counter so it does not run away.
            cleanDays = 0
        }

        return DayOutcome(
            verdict = verdict,
            state = previous.copy(
                currentStreak = streak,
                bestStreak = maxOf(previous.bestStreak, streak),
                perfectDays = previous.perfectDays + if (verdict == DayVerdict.PERFECT) 1 else 0,
                freezes = freezes,
                cleanDays = cleanDays
            ),
            allowanceApplied = allowance,
            freezeSpent = false,
            freezeEarned = earned
        )
    }

    /**
     * A freeze absorbed the day. The streak holds where it is; it does not grow. Progress toward
     * the next freeze is kept -- the bad day costs the freeze, nothing else.
     */
    private fun frozen(previous: StreakState, allowance: Int) = DayOutcome(
        verdict = DayVerdict.FROZEN,
        state = previous.copy(freezes = previous.freezes - 1),
        allowanceApplied = allowance,
        freezeSpent = true,
        freezeEarned = false
    )

    private fun broken(previous: StreakState, allowance: Int) = DayOutcome(
        verdict = DayVerdict.BROKEN,
        state = previous.copy(
            currentStreak = 0,
            bestStreak = maxOf(previous.bestStreak, previous.currentStreak),
            cleanDays = 0
        ),
        allowanceApplied = allowance,
        freezeSpent = false,
        freezeEarned = false
    )

    /** True when finishing what is left would be the difference between keeping and losing the streak. */
    fun streakAtRisk(state: StreakState, dueCount: Int, doneCount: Int): Boolean {
        if (state.currentStreak == 0 || dueCount == 0) return false
        val missedIfDayEndedNow = dueCount - doneCount
        return missedIfDayEndedNow > allowanceFor(dueCount)
    }
}
