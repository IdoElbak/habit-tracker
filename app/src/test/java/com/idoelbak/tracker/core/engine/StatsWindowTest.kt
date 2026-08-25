package com.idoelbak.tracker.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsWindowTest {

    private val sunday = DayOfWeek.SUNDAY
    private val tue = LocalDate.of(2026, 8, 25)   // Tuesday

    @Test
    fun `four weeks ends with the current week, not today`() {
        val range = StatsWindow.lastWeeks(tue, sunday)
        // Current week starts Sun 23 Aug; three weeks earlier is Sun 2 Aug.
        assertEquals(LocalDate.of(2026, 8, 2), range.start)
        // The row runs to Saturday even though Saturday has not happened yet.
        assertEquals(LocalDate.of(2026, 8, 29), range.endInclusive)
    }

    @Test
    fun `the grid is four rows of exactly seven days`() {
        val grid = StatsWindow.gridWeeks(tue, sunday)
        assertEquals(4, grid.size)
        grid.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun `every column of the grid is the same weekday`() {
        // This alignment is the entire reason for using weeks rather than a calendar month.
        val grid = StatsWindow.gridWeeks(tue, sunday)
        for (col in 0..6) {
            val weekdays = grid.map { it[col].dayOfWeek }.toSet()
            assertEquals("column $col drifted across weekdays", 1, weekdays.size)
        }
    }

    @Test
    fun `the grid starts on the chosen week start`() {
        StatsWindow.gridWeeks(tue, DayOfWeek.SUNDAY).forEach {
            assertEquals(DayOfWeek.SUNDAY, it.first().dayOfWeek)
        }
        StatsWindow.gridWeeks(tue, DayOfWeek.MONDAY).forEach {
            assertEquals(DayOfWeek.MONDAY, it.first().dayOfWeek)
        }
    }

    @Test
    fun `the grid rows run consecutively with no gaps`() {
        val flat = StatsWindow.gridWeeks(tue, sunday).flatten()
        assertEquals(28, flat.size)
        flat.zipWithNext { a, b -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun `today is inside the last row`() {
        val grid = StatsWindow.gridWeeks(tue, sunday)
        assertTrue(grid.last().contains(tue))
    }

    @Test
    fun `a grid anchored on the week start still shows the whole week`() {
        val sun = LocalDate.of(2026, 8, 23)
        val grid = StatsWindow.gridWeeks(sun, sunday)
        assertEquals(sun, grid.last().first())
        assertEquals(LocalDate.of(2026, 8, 29), grid.last().last())
    }

    @Test
    fun `weekday order follows the week start`() {
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
            ),
            StatsWindow.weekdayOrder(DayOfWeek.SUNDAY)
        )
        assertEquals(DayOfWeek.MONDAY, StatsWindow.weekdayOrder(DayOfWeek.MONDAY).first())
        assertEquals(DayOfWeek.SUNDAY, StatsWindow.weekdayOrder(DayOfWeek.MONDAY).last())
    }

    @Test
    fun `a rolling day window includes today and counts back inclusively`() {
        val range = StatsWindow.lastDays(tue, 91)
        assertEquals(tue, range.endInclusive)
        assertEquals(LocalDate.of(2026, 5, 27), range.start)
    }
}
