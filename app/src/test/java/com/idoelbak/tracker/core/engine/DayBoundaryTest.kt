package com.idoelbak.tracker.core.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class DayBoundaryTest {

    // 2026-08-25 is a Tuesday. Every date below is anchored to that week.
    private val tue = LocalDate.of(2026, 8, 25)
    private val sunOfThatWeek = LocalDate.of(2026, 8, 23)
    private val monOfThatWeek = LocalDate.of(2026, 8, 24)

    @Test
    fun `a tick at half past one still belongs to the previous day`() {
        val lateNight = LocalDateTime.of(2026, 8, 26, 1, 30)
        assertEquals(tue, DayBoundary.trackingDate(lateNight))
    }

    @Test
    fun `a tick just before rollover still belongs to the previous day`() {
        val justBefore = LocalDateTime.of(2026, 8, 26, 2, 59)
        assertEquals(tue, DayBoundary.trackingDate(justBefore))
    }

    @Test
    fun `a tick at rollover starts the new day`() {
        val atThree = LocalDateTime.of(2026, 8, 26, 3, 0)
        assertEquals(LocalDate.of(2026, 8, 26), DayBoundary.trackingDate(atThree))
    }

    @Test
    fun `a midnight rollover behaves like a plain calendar day`() {
        val lateNight = LocalDateTime.of(2026, 8, 26, 1, 30)
        assertEquals(LocalDate.of(2026, 8, 26), DayBoundary.trackingDate(lateNight, rolloverHour = 0))
    }

    @Test
    fun `week start is respected`() {
        assertEquals(sunOfThatWeek, DayBoundary.weekStartOf(tue, DayOfWeek.SUNDAY))
        assertEquals(monOfThatWeek, DayBoundary.weekStartOf(tue, DayOfWeek.MONDAY))
    }

    @Test
    fun `a day that is itself the week start returns itself`() {
        assertEquals(sunOfThatWeek, DayBoundary.weekStartOf(sunOfThatWeek, DayOfWeek.SUNDAY))
        assertEquals(7, DayBoundary.daysRemainingInWeek(sunOfThatWeek, DayOfWeek.SUNDAY))
    }

    @Test
    fun `days remaining counts today and shrinks to one on the last day`() {
        // Sunday-start week: Sun Mon Tue Wed Thu Fri Sat
        assertEquals(7, DayBoundary.daysRemainingInWeek(sunOfThatWeek, DayOfWeek.SUNDAY))
        assertEquals(5, DayBoundary.daysRemainingInWeek(tue, DayOfWeek.SUNDAY))
        assertEquals(1, DayBoundary.daysRemainingInWeek(LocalDate.of(2026, 8, 29), DayOfWeek.SUNDAY))
    }

    @Test
    fun `week end is six days after week start`() {
        assertEquals(LocalDate.of(2026, 8, 29), DayBoundary.weekEndOf(tue, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 8, 30), DayBoundary.weekEndOf(tue, DayOfWeek.MONDAY))
    }

    @Test
    fun `the day ends at rollover on the following morning`() {
        assertEquals(LocalDateTime.of(2026, 8, 26, 3, 0), DayBoundary.endOf(tue))
    }
}
