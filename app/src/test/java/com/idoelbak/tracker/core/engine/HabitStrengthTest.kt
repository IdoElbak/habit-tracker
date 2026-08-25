package com.idoelbak.tracker.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStrengthTest {

    @Test
    fun `a habit with no history scores zero`() {
        assertEquals(0.0, HabitStrength.compute(emptyList()), 1e-9)
    }

    @Test
    fun `never doing it keeps the score at zero`() {
        assertEquals(0.0, HabitStrength.compute(List(60) { false }), 1e-9)
    }

    @Test
    fun `the half-life is exactly what it claims`() {
        // Thirteen days of practice from a standing start closes exactly half the gap to 1.0.
        val after13 = HabitStrength.compute(List(13) { true })
        assertEquals(0.5, after13, 1e-9)

        val after26 = HabitStrength.compute(List(26) { true })
        assertEquals(0.75, after26, 1e-9)
    }

    @Test
    fun `sustained practice approaches but never exceeds one`() {
        val score = HabitStrength.compute(List(400) { true })
        assertTrue(score > 0.99)
        assertTrue(score <= 1.0)
    }

    @Test
    fun `one missed day dents the score without erasing it`() {
        val solid = HabitStrength.compute(List(120) { true })
        val withOneMiss = HabitStrength.compute(List(120) { true } + false)

        assertTrue("a miss must cost something", withOneMiss < solid)
        assertTrue("but nowhere near everything -- this is the whole point", withOneMiss > solid - 0.06)
        assertTrue(withOneMiss > 0.9)
    }

    @Test
    fun `a bad week hurts materially but survives`() {
        val solid = HabitStrength.compute(List(120) { true })
        val badWeek = HabitStrength.compute(List(120) { true } + List(7) { false })

        assertTrue(badWeek < solid - 0.2)
        assertTrue("months of work are not wiped out by one bad week", badWeek > 0.65)
    }

    @Test
    fun `a broken streak still leaves visible strength`() {
        // The case the metric exists for: streak is 0, but the habit is plainly not dead.
        val score = HabitStrength.percent(List(90) { true } + List(3) { false })
        assertTrue("streak would read 0 here", score > 80)
    }

    @Test
    fun `stepping one day at a time matches recomputing the whole history`() {
        val history = listOf(true, true, false, true, true, true, false, false, true, true, true)
        var stepped = 0.0
        history.forEach { stepped = HabitStrength.step(stepped, it) }
        assertEquals(HabitStrength.compute(history), stepped, 1e-12)
    }

    @Test
    fun `percent rounds to a whole number in range`() {
        assertEquals(50, HabitStrength.percent(List(13) { true }))
        assertEquals(0, HabitStrength.percent(emptyList()))
    }
}
