package com.idoelbak.tracker.data

import com.idoelbak.tracker.core.engine.DayBoundary
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.db.CompletionEntity
import com.idoelbak.tracker.data.db.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What lands on the Today screen.
 *
 * The rule under test is Ido's: a habit that is not due today does not appear on Today at all, and
 * comes back on its own day. It is always on the Habits page in the meantime.
 */
class TodayUiTest {

    private val weekStart = DayOfWeek.SUNDAY

    // Sunday 23 August 2026 through Saturday 29 August.
    private val sunday = LocalDate.of(2026, 8, 23)
    private val monday = sunday.plusDays(1)
    private val tuesday = sunday.plusDays(2)

    private fun habit(
        id: Long,
        name: String,
        type: ScheduleType,
        times: Int = 0,
        mask: Int = 0
    ) = HabitEntity(
        id = id,
        name = name,
        scheduleType = type,
        timesPerWeek = times,
        weekdayMask = mask,
        createdAt = 0L
    )

    private fun on(date: LocalDate, habits: List<HabitEntity>, ticks: List<CompletionEntity>) =
        buildToday(
            date = date,
            weekFrom = DayBoundary.weekStartOf(date, weekStart),
            habits = habits,
            ticks = ticks,
            streak = null,
            rating = null,
            weekStart = weekStart
        )

    private fun tick(habitId: Long, date: LocalDate) = CompletionEntity(habitId, date, 0L)

    private val gym = habit(
        1, "Gym", ScheduleType.SPECIFIC_DAYS,
        mask = (1 shl (DayOfWeek.MONDAY.value - 1)) or (1 shl (DayOfWeek.WEDNESDAY.value - 1))
    )
    private val read = habit(2, "Read", ScheduleType.DAILY)
    private val draw = habit(3, "Draw", ScheduleType.TIMES_PER_WEEK, times = 3)

    @Test
    fun `a habit due on other days is nowhere on today`() {
        val ui = on(tuesday, listOf(gym, read), emptyList())
        assertEquals(listOf("Read"), ui.due.map { it.name })
        assertTrue("a resting habit is not smuggled into the weekly list", ui.weekly.isEmpty())
        assertEquals(1, ui.dueCount)
    }

    @Test
    fun `the same habit is back on the day it is due`() {
        val ui = on(monday, listOf(gym, read), emptyList())
        assertEquals(listOf("Gym", "Read"), ui.due.map { it.name })
    }

    @Test
    fun `a weekly quota habit is optional while it has slack and required once it runs out`() {
        val early = on(sunday, listOf(draw), emptyList())
        assertTrue("Sunday leaves six days for three sessions", early.due.isEmpty())
        assertEquals(listOf("Draw"), early.weekly.map { it.name })
        assertEquals(Quota(0, 3), early.weekly.single().quota)

        // Wednesday still leaves four days for three sessions -- slack.
        val wednesday = sunday.plusDays(3)
        assertTrue(on(wednesday, listOf(draw), emptyList()).due.isEmpty())

        // Thursday: three owed, three days left. It stops being a choice.
        val thursday = sunday.plusDays(4)
        assertEquals(listOf("Draw"), on(thursday, listOf(draw), emptyList()).due.map { it.name })
    }

    @Test
    fun `ticking a resting habit anyway keeps it visible for the rest of the day`() {
        val ui = on(tuesday, listOf(gym), listOf(tick(gym.id, tuesday)))
        assertTrue("an off-day tick is a bonus, not a duty", ui.due.isEmpty())
        assertEquals(listOf("Gym"), ui.weekly.map { it.name })
        assertTrue(ui.weekly.single().done)
    }

    @Test
    fun `the week page measures every habit against its own full week`() {
        val ticks = listOf(
            tick(read.id, sunday), tick(read.id, monday), tick(read.id, tuesday),
            tick(draw.id, monday),
            tick(gym.id, monday)
        )
        val week = buildWeek(
            date = tuesday,
            weekFrom = DayBoundary.weekStartOf(tuesday, weekStart),
            habits = listOf(gym, read, draw),
            ticks = ticks,
            weekStart = weekStart
        )

        // A daily habit's week is 7, a quota habit's is its quota, a weekday habit's is its days.
        assertEquals(listOf(2, 7, 3), week.rows.map { it.goal })
        assertEquals(listOf(1, 3, 1), week.rows.map { it.done })
        assertEquals(5, week.done)
        assertEquals(12, week.goal)
        assertEquals("half a two-day week", 0.5f, week.rows.first().fraction, 0.001f)
    }

    @Test
    fun `a forgotten tick can be fixed for a week, and no further`() {
        assertTrue("today", Backfill.canEdit(tuesday, tuesday))
        assertTrue("yesterday", Backfill.canEdit(monday, tuesday))
        assertTrue("the sixth day back is the last one", Backfill.canEdit(tuesday.minusDays(6), tuesday))
        assertFalse("a week and a day is history", Backfill.canEdit(tuesday.minusDays(7), tuesday))
        assertFalse("tomorrow has not happened", Backfill.canEdit(tuesday.plusDays(1), tuesday))
    }

    @Test
    fun `a met quota drops off today but a missed day never shows as a miss`() {
        val ticks = listOf(tick(draw.id, sunday), tick(draw.id, monday), tick(draw.id, tuesday))
        val wednesday = sunday.plusDays(3)
        val ui = on(wednesday, listOf(draw), ticks)
        assertTrue("three of three done: nothing left to push", ui.due.isEmpty() && ui.weekly.isEmpty())

        // The gym's Tuesday is a rest day, so its week strip must not read as a miss.
        val strip = on(wednesday, listOf(gym), emptyList())
        val dots = strip.due.single().week
        assertEquals(DayDot.IDLE, dots[0])   // Sunday: not a gym day
        assertEquals(DayDot.MISS, dots[1])   // Monday: was due, not done
        assertEquals(DayDot.IDLE, dots[2])   // Tuesday: not a gym day
        assertEquals(DayDot.TODAY, dots[3])  // Wednesday: due, still open
        assertFalse(dots.drop(4).any { it != DayDot.FUTURE })
    }
}
