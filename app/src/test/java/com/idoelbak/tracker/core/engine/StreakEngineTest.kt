package com.idoelbak.tracker.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakEngineTest {

    private fun fresh(
        streak: Int = 0,
        best: Int = 0,
        perfect: Int = 0,
        freezes: Int = 0,
        run: Int = 0
    ) = StreakState(streak, best, perfect, freezes, run)

    // ---- the allowance boundary -------------------------------------------------------------

    @Test
    fun `on a light day everything is required`() {
        assertEquals(0, StreakEngine.allowanceFor(1))
        assertEquals(0, StreakEngine.allowanceFor(3))
        val out = StreakEngine.closeDay(fresh(streak = 5, freezes = 0), dueCount = 3, doneCount = 2)
        assertEquals(DayVerdict.BROKEN, out.verdict)
    }

    @Test
    fun `from four due upward one miss is forgiven`() {
        assertEquals(1, StreakEngine.allowanceFor(4))
        assertEquals(1, StreakEngine.allowanceFor(11))
        val out = StreakEngine.closeDay(fresh(streak = 5), dueCount = 4, doneCount = 3)
        assertEquals(DayVerdict.COMPLETE, out.verdict)
        assertEquals(6, out.state.currentStreak)
    }

    @Test
    fun `two misses is never within the allowance`() {
        val out = StreakEngine.closeDay(fresh(streak = 5, freezes = 0), dueCount = 11, doneCount = 9)
        assertEquals(DayVerdict.BROKEN, out.verdict)
    }

    // ---- verdicts ---------------------------------------------------------------------------

    @Test
    fun `a perfect day counts as perfect and extends the streak`() {
        val out = StreakEngine.closeDay(fresh(streak = 13, best = 23, perfect = 8), dueCount = 7, doneCount = 7)
        assertEquals(DayVerdict.PERFECT, out.verdict)
        assertEquals(14, out.state.currentStreak)
        assertEquals(23, out.state.bestStreak)
        assertEquals(9, out.state.perfectDays)
    }

    @Test
    fun `a complete-but-not-perfect day does not raise the perfect count`() {
        val out = StreakEngine.closeDay(fresh(streak = 13, perfect = 8), dueCount = 7, doneCount = 6)
        assertEquals(DayVerdict.COMPLETE, out.verdict)
        assertEquals(14, out.state.currentStreak)
        assertEquals(8, out.state.perfectDays)
    }

    @Test
    fun `passing the old best raises it`() {
        val out = StreakEngine.closeDay(fresh(streak = 23, best = 23), dueCount = 5, doneCount = 5)
        assertEquals(24, out.state.currentStreak)
        assertEquals(24, out.state.bestStreak)
    }

    @Test
    fun `a day with nothing due changes nothing at all`() {
        val before = fresh(streak = 14, best = 23, perfect = 9, freezes = 1, run = 4)
        val out = StreakEngine.closeDay(before, dueCount = 0, doneCount = 0)
        assertEquals(DayVerdict.EMPTY, out.verdict)
        assertEquals(before, out.state)
    }

    // ---- freezes ----------------------------------------------------------------------------

    @Test
    fun `a freeze absorbs a bad day and holds the streak without extending it`() {
        val out = StreakEngine.closeDay(fresh(streak = 14, best = 23, freezes = 2, run = 5), dueCount = 11, doneCount = 8)
        assertEquals(DayVerdict.FROZEN, out.verdict)
        assertTrue(out.freezeSpent)
        assertEquals("a frozen day is survived, not earned", 14, out.state.currentStreak)
        assertEquals(1, out.state.freezes)
        assertEquals("progress toward the next freeze resets", 0, out.state.perfectRun)
    }

    @Test
    fun `two bad days in a row spend both freezes and the streak still stands`() {
        var state = fresh(streak = 14, best = 23, freezes = 2)
        // Ido's scenario: 8 of 11 on two consecutive days.
        val day1 = StreakEngine.closeDay(state, dueCount = 11, doneCount = 8)
        state = day1.state
        val day2 = StreakEngine.closeDay(state, dueCount = 11, doneCount = 8)
        state = day2.state

        assertEquals(DayVerdict.FROZEN, day1.verdict)
        assertEquals(DayVerdict.FROZEN, day2.verdict)
        assertEquals(14, state.currentStreak)
        assertEquals(0, state.freezes)
    }

    @Test
    fun `a third bad day with no freeze left breaks the streak`() {
        val out = StreakEngine.closeDay(fresh(streak = 14, best = 23, freezes = 0), dueCount = 11, doneCount = 8)
        assertEquals(DayVerdict.BROKEN, out.verdict)
        assertEquals(0, out.state.currentStreak)
        assertEquals("the run is remembered as the best", 23, out.state.bestStreak)
    }

    @Test
    fun `breaking a streak longer than the old best records the new best`() {
        val out = StreakEngine.closeDay(fresh(streak = 30, best = 23, freezes = 0), dueCount = 5, doneCount = 1)
        assertEquals(DayVerdict.BROKEN, out.verdict)
        assertEquals(30, out.state.bestStreak)
    }

    @Test
    fun `a freeze comes back after exactly seven perfect days`() {
        var state = fresh(streak = 0, freezes = 0, run = 0)
        repeat(6) {
            val out = StreakEngine.closeDay(state, dueCount = 5, doneCount = 5)
            state = out.state
            assertFalse("no freeze before the seventh day", out.freezeEarned)
        }
        assertEquals(6, state.perfectRun)
        assertEquals(0, state.freezes)

        val seventh = StreakEngine.closeDay(state, dueCount = 5, doneCount = 5)
        assertTrue(seventh.freezeEarned)
        assertEquals(1, seventh.state.freezes)
        assertEquals(0, seventh.state.perfectRun)
    }

    @Test
    fun `days saved by the allowance do not buy a freeze back`() {
        // Seven days that each leaned on the allowance keep the streak, but earn nothing back.
        var state = fresh(streak = 10, freezes = 0, run = 0)
        repeat(7) {
            val out = StreakEngine.closeDay(state, dueCount = 5, doneCount = 4)
            assertEquals(DayVerdict.COMPLETE, out.verdict)
            assertFalse("only perfect days count toward a freeze", out.freezeEarned)
            state = out.state
        }
        assertEquals(0, state.perfectRun)
        assertEquals(0, state.freezes)
        assertEquals("the streak itself still ran", 17, state.currentStreak)

        // A complete day in the middle of a perfect run holds the count rather than wiping it.
        repeat(6) { state = StreakEngine.closeDay(state, dueCount = 5, doneCount = 5).state }
        state = StreakEngine.closeDay(state, dueCount = 5, doneCount = 4).state
        assertEquals(6, state.perfectRun)
        val seventh = StreakEngine.closeDay(state, dueCount = 5, doneCount = 5)
        assertTrue(seventh.freezeEarned)
    }

    @Test
    fun `freezes are capped at two`() {
        var state = fresh(freezes = 2, run = 0)
        repeat(14) { state = StreakEngine.closeDay(state, dueCount = 5, doneCount = 5).state }
        assertEquals(StreakEngine.MAX_FREEZES, state.freezes)
    }

    @Test
    fun `spending a freeze then running perfect earns it back in two weeks`() {
        // The recovery Ido described: two bad days, then back where you were.
        var state = fresh(streak = 14, best = 23, freezes = 2)
        state = StreakEngine.closeDay(state, dueCount = 11, doneCount = 8).state
        state = StreakEngine.closeDay(state, dueCount = 11, doneCount = 8).state
        assertEquals(0, state.freezes)

        repeat(7) { state = StreakEngine.closeDay(state, dueCount = 11, doneCount = 11).state }
        assertEquals(1, state.freezes)

        repeat(7) { state = StreakEngine.closeDay(state, dueCount = 11, doneCount = 11).state }
        assertEquals(2, state.freezes)
        assertEquals("14 perfect days on top of the surviving streak", 28, state.currentStreak)
    }

    // ---- at-risk signal ---------------------------------------------------------------------

    @Test
    fun `the streak is at risk only when finishing up is what saves it`() {
        val state = fresh(streak = 14, freezes = 2)
        assertFalse("within the allowance", StreakEngine.streakAtRisk(state, dueCount = 7, doneCount = 6))
        assertTrue("two short on a seven-habit day", StreakEngine.streakAtRisk(state, dueCount = 7, doneCount = 5))
        assertTrue("one short on a three-habit day", StreakEngine.streakAtRisk(state, dueCount = 3, doneCount = 2))
        assertFalse("nothing due", StreakEngine.streakAtRisk(state, dueCount = 0, doneCount = 0))
        assertFalse("no streak to lose", StreakEngine.streakAtRisk(fresh(), dueCount = 7, doneCount = 0))
    }

    // ---- guards -----------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `more done than due is rejected`() {
        StreakEngine.closeDay(fresh(), dueCount = 3, doneCount = 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a negative due count is rejected`() {
        StreakEngine.closeDay(fresh(), dueCount = -1, doneCount = 0)
    }
}
