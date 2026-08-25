package com.idoelbak.tracker.core.engine

import com.idoelbak.tracker.core.model.DueState
import com.idoelbak.tracker.core.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DueCalculatorTest {

    private val sunday = DayOfWeek.SUNDAY

    // The week of 2026-08-23 (Sun) .. 2026-08-29 (Sat).
    private val sun = LocalDate.of(2026, 8, 23)
    private val mon = LocalDate.of(2026, 8, 24)
    private val tue = LocalDate.of(2026, 8, 25)
    private val wed = LocalDate.of(2026, 8, 26)
    private val thu = LocalDate.of(2026, 8, 27)
    private val fri = LocalDate.of(2026, 8, 28)
    private val sat = LocalDate.of(2026, 8, 29)

    @Test
    fun `a daily habit is due every day`() {
        listOf(sun, mon, tue, wed, thu, fri, sat).forEach { day ->
            assertEquals("$day", DueState.DUE, DueCalculator.dueState(Schedule.daily(), day, sunday))
        }
    }

    @Test
    fun `a fixed-day habit is due only on its days`() {
        val gym = Schedule.onDays(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        assertEquals(DueState.NOT_DUE, DueCalculator.dueState(gym, sun, sunday))
        assertEquals(DueState.DUE, DueCalculator.dueState(gym, mon, sunday))
        assertEquals(DueState.NOT_DUE, DueCalculator.dueState(gym, tue, sunday))
        assertEquals(DueState.DUE, DueCalculator.dueState(gym, wed, sunday))
        assertEquals(DueState.DUE, DueCalculator.dueState(gym, thu, sunday))
        assertEquals(DueState.NOT_DUE, DueCalculator.dueState(gym, fri, sunday))
        assertEquals(DueState.DUE, DueCalculator.dueState(gym, sat, sunday))
        assertEquals(4, gym.weeklyGoal)
    }

    @Test
    fun `a fixed-day habit is unaffected by which day the week starts on`() {
        val gym = Schedule.onDays(DayOfWeek.MONDAY)
        assertEquals(DueState.DUE, DueCalculator.dueState(gym, mon, DayOfWeek.SUNDAY))
        assertEquals(DueState.DUE, DueCalculator.dueState(gym, mon, DayOfWeek.MONDAY))
    }

    @Test
    fun `a weekly-quota habit stays optional while there is slack`() {
        val workout = Schedule.timesPerWeek(3)
        // 3 owed, 7 days left
        assertEquals(DueState.OPEN, DueCalculator.dueState(workout, sun, sunday, completionsEarlierInWeek = 0))
        // 3 owed, 5 days left
        assertEquals(DueState.OPEN, DueCalculator.dueState(workout, tue, sunday, completionsEarlierInWeek = 0))
        // 3 owed, 4 days left
        assertEquals(DueState.OPEN, DueCalculator.dueState(workout, wed, sunday, completionsEarlierInWeek = 0))
    }

    @Test
    fun `a weekly-quota habit becomes mandatory when owed equals days left`() {
        val workout = Schedule.timesPerWeek(3)
        // 3 owed, 3 days left (Thu Fri Sat) -- no slack remains
        assertEquals(DueState.DUE, DueCalculator.dueState(workout, thu, sunday, completionsEarlierInWeek = 0))
        // 2 owed, 2 days left
        assertEquals(DueState.DUE, DueCalculator.dueState(workout, fri, sunday, completionsEarlierInWeek = 1))
        // 1 owed, 1 day left
        assertEquals(DueState.DUE, DueCalculator.dueState(workout, sat, sunday, completionsEarlierInWeek = 2))
    }

    @Test
    fun `a weekly-quota habit drops out once the quota is met`() {
        val workout = Schedule.timesPerWeek(3)
        assertEquals(DueState.NOT_DUE, DueCalculator.dueState(workout, thu, sunday, completionsEarlierInWeek = 3))
        assertEquals(DueState.NOT_DUE, DueCalculator.dueState(workout, sat, sunday, completionsEarlierInWeek = 4))
    }

    @Test
    fun `escalation fires exactly once, on the day slack runs out`() {
        val workout = Schedule.timesPerWeek(3)
        assertFalse(DueCalculator.justEscalated(workout, wed, sunday, completionsEarlierInWeek = 0))
        assertTrue(DueCalculator.justEscalated(workout, thu, sunday, completionsEarlierInWeek = 0))
        // Still DUE on Friday, but it did not *become* due that day -- no second notification.
        assertFalse(DueCalculator.justEscalated(workout, fri, sunday, completionsEarlierInWeek = 0))
    }

    @Test
    fun `escalation never fires on the first day of the week`() {
        val everyDayQuota = Schedule.timesPerWeek(7)
        // 7 owed with 7 days left is DUE from the off, but nothing escalated into it.
        assertEquals(DueState.DUE, DueCalculator.dueState(everyDayQuota, sun, sunday, completionsEarlierInWeek = 0))
        assertFalse(DueCalculator.justEscalated(everyDayQuota, sun, sunday, completionsEarlierInWeek = 0))
    }

    @Test
    fun `escalation does not fire for daily or fixed-day habits`() {
        assertFalse(DueCalculator.justEscalated(Schedule.daily(), thu, sunday, 0))
        assertFalse(DueCalculator.justEscalated(Schedule.onDays(DayOfWeek.THURSDAY), thu, sunday, 0))
    }

    @Test
    fun `week boundary is honoured when the week starts on Monday`() {
        val workout = Schedule.timesPerWeek(3)
        // Monday-start week: Sunday is the LAST day, so 3 owed with 1 day left is mandatory.
        assertEquals(DueState.DUE, DueCalculator.dueState(workout, sun, DayOfWeek.MONDAY, completionsEarlierInWeek = 0))
        // Sunday-start week: the same Sunday is the FIRST day, with all the slack in the world.
        assertEquals(DueState.OPEN, DueCalculator.dueState(workout, sun, DayOfWeek.SUNDAY, completionsEarlierInWeek = 0))
    }

    @Test
    fun `still owed never goes negative`() {
        assertEquals(0, DueCalculator.stillOwedThisWeek(Schedule.timesPerWeek(3), completionsThisWeek = 5))
        assertEquals(2, DueCalculator.stillOwedThisWeek(Schedule.timesPerWeek(3), completionsThisWeek = 1))
        assertEquals(3, DueCalculator.stillOwedThisWeek(Schedule.daily(), completionsThisWeek = 4))
    }
}
