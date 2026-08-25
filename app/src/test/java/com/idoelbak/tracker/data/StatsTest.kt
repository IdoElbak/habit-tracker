package com.idoelbak.tracker.data

import com.idoelbak.tracker.core.engine.DayVerdict
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.db.DayRecordEntity
import com.idoelbak.tracker.data.db.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsTest {

    private val weekStart = DayOfWeek.SUNDAY
    private val today = LocalDate.of(2026, 8, 26)  // a Wednesday

    private fun record(date: LocalDate, due: Int, done: Int) = DayRecordEntity(
        date = date,
        dueCount = due,
        doneCount = done,
        verdict = if (due > 0 && due == done) DayVerdict.PERFECT else DayVerdict.COMPLETE,
        allowanceApplied = 1,
        freezeSpent = false,
        freezeEarned = false,
        streakAfter = 1,
        closedAt = 0L
    )

    private fun stats(records: List<DayRecordEntity>, period: StatsPeriod = StatsPeriod.MONTH) =
        buildStats(
            today = today,
            weekStart = weekStart,
            period = period,
            records = records,
            ratings = emptyList(),
            habits = emptyList(),
            ticks = emptyList(),
            streak = null
        )

    @Test
    fun `an app with no settled days says nothing rather than zero`() {
        val ui = stats(emptyList())
        assertNull("0% would be a lie on day one", ui.completionPercent)
        assertEquals(0, ui.daysTracked)
        assertTrue(ui.trend.isEmpty())
    }

    @Test
    fun `days with nothing due are left out of the percentage`() {
        val ui = stats(
            listOf(
                record(today.minusDays(1), due = 4, done = 2),
                record(today.minusDays(2), due = 0, done = 0),
                record(today.minusDays(3), due = 4, done = 4)
            )
        )
        assertEquals("6 of 8, and the empty day is not a zero", 75, ui.completionPercent)
        assertEquals(2, ui.daysTracked)
        assertEquals(6, ui.completions)
    }

    @Test
    fun `the period selector actually narrows the window`() {
        val records = (1..20).map { record(today.minusDays(it.toLong()), due = 2, done = if (it <= 3) 2 else 0) }
        // This week: Sunday to today. Only the three most recent days fall inside it, all perfect.
        assertEquals(100, stats(records, StatsPeriod.WEEK).completionPercent)
        // A month back sweeps in the failures too.
        assertEquals(15, stats(records, StatsPeriod.MONTH).completionPercent)
    }

    @Test
    fun `a weak weekday is only called out when the gap is real`() {
        val even = (1..21).map { record(today.minusDays(it.toLong()), due = 2, done = 2) }
        assertNull("no weekday stands out", stats(even).weakestDayNote)

        val saturdaysBad = (1..21).map {
            val date = today.minusDays(it.toLong())
            record(date, due = 2, done = if (date.dayOfWeek == DayOfWeek.SATURDAY) 0 else 2)
        }
        val note = stats(saturdaysBad).weakestDayNote
        assertNotNull(note)
        assertTrue("names the day and the size of the gap", note!!.startsWith("Saturday is your weakest day by"))
    }

    @Test
    fun `the heat grid is four weeks of aligned weekday columns`() {
        val ui = stats(listOf(record(today.minusDays(1), due = 4, done = 4)))
        assertEquals(4, ui.grid.size)
        assertTrue(ui.grid.all { it.size == 7 })
        assertTrue("every column is one weekday", ui.grid.map { it[3].date.dayOfWeek }.distinct().size == 1)
        // The last row is Sunday 23rd to Saturday 29th; yesterday is the Tuesday in column 2.
        assertEquals("a full day is the strongest step", 4, ui.grid.last()[2].level)
        assertTrue("tomorrow is not painted as a miss", ui.grid.last()[4].future)
    }

    @Test
    fun `habit strength skips the days a habit was not due`() {
        val gym = HabitEntity(
            id = 1,
            name = "Gym",
            scheduleType = ScheduleType.SPECIFIC_DAYS,
            weekdayMask = 1 shl (DayOfWeek.MONDAY.value - 1),
            createdAt = 0L
        )
        // Every Monday for the last twelve weeks was done; every other day is not counted at all.
        val mondays = (1..84L)
            .map { today.minusDays(it) }
            .filter { it.dayOfWeek == DayOfWeek.MONDAY }
            .map { com.idoelbak.tracker.data.db.CompletionEntity(gym.id, it, 0L) }

        val ui = buildStats(
            today = today,
            weekStart = weekStart,
            period = StatsPeriod.MONTH,
            records = emptyList(),
            ratings = emptyList(),
            habits = listOf(gym),
            ticks = mondays,
            streak = null
        )
        assertTrue(
            "twelve straight Mondays is a strong habit, not a 1-in-7 one",
            ui.strength.single().percent >= 45
        )
    }
}
