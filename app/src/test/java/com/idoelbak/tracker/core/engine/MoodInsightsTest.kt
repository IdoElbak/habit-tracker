package com.idoelbak.tracker.core.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MoodInsightsTest {

    private fun day(rating: Int?, due: Int, done: Int) = RatedDay(rating, due, done)

    @Test
    fun `not enough rated days yields no finding`() {
        val days = List(3) { day(2, 10, 3) } + List(10) { day(9, 10, 9) }
        assertNull("only three low days is not a pattern", MoodInsights.compare(days))
    }

    @Test
    fun `unrated days are ignored rather than treated as zero`() {
        val days = List(20) { day(null, 10, 0) } + List(5) { day(2, 10, 5) } + List(5) { day(9, 10, 9) }
        val finding = MoodInsights.compare(days)
        assertNotNull(finding)
        assertEquals(5, finding!!.lowDays)
        assertEquals(5, finding.highDays)
    }

    @Test
    fun `days with nothing due are ignored`() {
        val days = List(6) { day(2, 0, 0) } + List(6) { day(9, 0, 0) }
        assertNull("no completion rate exists on an empty day", MoodInsights.compare(days))
    }

    @Test
    fun `a small difference is not reported as a finding`() {
        val days = List(6) { day(2, 10, 8) } + List(6) { day(9, 10, 8) }
        assertNull("identical behaviour is not an insight", MoodInsights.compare(days))

        val slight = List(6) { day(2, 100, 76) } + List(6) { day(9, 100, 80) }
        assertNull("four points is noise", MoodInsights.compare(slight))
    }

    @Test
    fun `a real gap is reported with both averages`() {
        val days = List(6) { day(2, 10, 6) } + List(8) { day(9, 10, 9) }
        val finding = MoodInsights.compare(days)!!

        assertEquals(6, finding.lowDays)
        assertEquals(8, finding.highDays)
        assertEquals(60, finding.lowCompletionPercent)
        assertEquals(90, finding.highCompletionPercent)
        assertEquals(30, finding.gapPercentagePoints)
        assertEquals("30 of 90 is a third less", 33, finding.relativeDropPercent)
    }

    @Test
    fun `middling ratings sit in neither bucket`() {
        // 5 and 6 are neither low nor high, so they must not pad either side.
        val days = List(20) { day(5, 10, 1) } + List(5) { day(2, 10, 6) } + List(5) { day(9, 10, 9) }
        val finding = MoodInsights.compare(days)!!
        assertEquals(5, finding.lowDays)
        assertEquals(5, finding.highDays)
        assertEquals(60, finding.lowCompletionPercent)
    }

    @Test
    fun `a finding can run the other way`() {
        // Doing the work on bad days anyway -- worth surfacing, not hiding.
        val days = List(6) { day(2, 10, 9) } + List(6) { day(9, 10, 5) }
        val finding = MoodInsights.compare(days)!!
        assertEquals(-40, finding.gapPercentagePoints)
    }

    @Test
    fun `average rating skips unrated days`() {
        val days = listOf(day(4, 5, 5), day(null, 5, 5), day(8, 5, 5))
        assertEquals(6.0, MoodInsights.averageRating(days)!!, 1e-9)
        assertNull(MoodInsights.averageRating(listOf(day(null, 5, 5))))
    }

    @Test
    fun `boundary ratings land in the right buckets`() {
        val days = List(5) { day(MoodInsights.LOW_MAX, 10, 5) } + List(5) { day(MoodInsights.HIGH_MIN, 10, 10) }
        val finding = MoodInsights.compare(days)!!
        assertEquals(5, finding.lowDays)
        assertEquals(5, finding.highDays)
    }
}
