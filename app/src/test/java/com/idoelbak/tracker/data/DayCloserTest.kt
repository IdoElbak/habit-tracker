package com.idoelbak.tracker.data

import com.idoelbak.tracker.core.engine.DayVerdict
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.db.DayRecordEntity
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.data.db.StreakStateEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class DayCloserTest {

    private val sunday = DayOfWeek.SUNDAY
    private val weekSun = LocalDate.of(2026, 8, 23)
    private val today = LocalDate.of(2026, 8, 27)   // Thursday

    private val createdAt = weekSun.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun habit(id: Long, name: String, schedule: Schedule = Schedule.daily()) = HabitEntity(
        id = id,
        name = name,
        scheduleType = schedule.type,
        timesPerWeek = schedule.timesPerWeek,
        weekdayMask = schedule.weekdayMask,
        createdAt = createdAt
    )

    /** Collects what the closer writes so the test can assert on it. */
    private class Sink {
        val records = mutableListOf<DayRecordEntity>()
        var state: StreakStateEntity? = null
    }

    private fun closer(
        habits: List<HabitEntity>,
        ticks: List<Pair<Long, LocalDate>>,
        sink: Sink,
        startingState: StreakStateEntity? = null,
        preExisting: Set<LocalDate> = emptySet()
    ): DayCloser {
        sink.state = startingState
        return DayCloser(
            habitsOn = { habits },
            completionsBetween = { from, to -> ticks.filter { it.second in from..to } },
            recordExists = { d -> d in preExisting || sink.records.any { it.date == d } },
            writeRecord = { sink.records += it },
            readState = { sink.state },
            writeState = { sink.state = it },
            weekStart = sunday,
            now = { 0L }
        )
    }

    @Test
    fun `today is never closed`() = runTest {
        val sink = Sink()
        val habits = listOf(habit(1, "Read"))
        val ticks = (0..4).map { 1L to weekSun.plusDays(it.toLong()) }

        closer(habits, ticks, sink).settleThrough(today)

        assertTrue("today is still being lived", sink.records.none { it.date == today })
        assertEquals(today.minusDays(1), sink.records.maxOf { it.date })
        assertEquals(today.minusDays(1), sink.state!!.lastClosedDate)
    }

    @Test
    fun `it settles every day from creation up to yesterday`() = runTest {
        val sink = Sink()
        val habits = listOf(habit(1, "Read"))
        val ticks = listOf(1L to weekSun, 1L to weekSun.plusDays(1))

        val closed = closer(habits, ticks, sink).settleThrough(today)

        // Sun 23, Mon 24, Tue 25, Wed 26 -- four days, Thursday excluded.
        assertEquals(4, closed)
        assertEquals(listOf(23, 24, 25, 26), sink.records.map { it.date.dayOfMonth })
    }

    @Test
    fun `a day already recorded is never rewritten`() = runTest {
        val sink = Sink()
        val habits = listOf(habit(1, "Read"))

        closer(habits, emptyList(), sink, preExisting = setOf(weekSun, weekSun.plusDays(1)))
            .settleThrough(today)

        assertTrue("pre-existing days must be left alone", sink.records.none { it.date == weekSun })
        assertEquals(listOf(25, 26), sink.records.map { it.date.dayOfMonth })
    }

    @Test
    fun `it resumes from the last closed day rather than starting over`() = runTest {
        val sink = Sink()
        val habits = listOf(habit(1, "Read"))
        val ticks = (0..4).map { 1L to weekSun.plusDays(it.toLong()) }

        val closed = closer(
            habits, ticks, sink,
            startingState = StreakStateEntity(currentStreak = 3, lastClosedDate = weekSun.plusDays(1))
        ).settleThrough(today)

        assertEquals(2, closed)
        assertEquals(listOf(25, 26), sink.records.map { it.date.dayOfMonth })
        assertEquals("the streak carried forward", 5, sink.state!!.currentStreak)
    }

    @Test
    fun `with no habits there is nothing to settle`() = runTest {
        val sink = Sink()
        val closed = closer(emptyList(), emptyList(), sink).settleThrough(today)

        assertEquals(0, closed)
        assertTrue(sink.records.isEmpty())
        assertNull("no phantom streak state on a fresh install", sink.state)
    }

    @Test
    fun `a rest day for a fixed-day habit is not a miss`() = runTest {
        val sink = Sink()
        // Gym on Mondays only. Sunday, Tuesday and Wednesday have nothing due at all.
        val habits = listOf(habit(1, "Workout", Schedule.onDays(DayOfWeek.MONDAY)))
        val ticks = listOf(1L to weekSun.plusDays(1))   // did it on the Monday

        closer(habits, ticks, sink).settleThrough(today)

        val byDay = sink.records.associateBy { it.date.dayOfMonth }
        assertEquals(DayVerdict.EMPTY, byDay[23]!!.verdict)
        assertEquals(DayVerdict.PERFECT, byDay[24]!!.verdict)
        assertEquals(DayVerdict.EMPTY, byDay[25]!!.verdict)
        assertEquals("rest days must not break anything", 1, sink.state!!.currentStreak)
    }

    @Test
    fun `a weekly-quota habit only counts once it has escalated`() = runTest {
        val sink = Sink()
        // 3 per week, never done. Sun-Wed have slack, so nothing is due yet.
        val habits = listOf(habit(1, "Workout", Schedule.timesPerWeek(3)))

        closer(habits, emptyList(), sink).settleThrough(today)

        sink.records.forEach {
            assertEquals("day ${it.date} should have had nothing mandatory", 0, it.dueCount)
            assertEquals(DayVerdict.EMPTY, it.verdict)
        }
    }

    @Test
    fun `counts only include what was actually mandatory`() = runTest {
        val sink = Sink()
        val habits = listOf(
            habit(1, "Read"),                                              // daily -- due
            habit(2, "Workout", Schedule.onDays(DayOfWeek.MONDAY)),        // not Tuesday
            habit(3, "Draw", Schedule.timesPerWeek(2))                     // still slack on Tuesday
        )
        val ticks = listOf(1L to weekSun.plusDays(2))

        val (due, done) = closer(habits, ticks, sink).countsFor(weekSun.plusDays(2))

        assertEquals("only the daily habit was mandatory", 1, due)
        assertEquals(1, done)
    }

    @Test
    fun `a missed day with freezes available is frozen, not broken`() = runTest {
        val sink = Sink()
        val habits = (1L..5L).map { habit(it, "Habit $it") }
        // Everything done on Sunday and Monday; nothing at all on Tuesday.
        val ticks = (1L..5L).flatMap { id -> listOf(id to weekSun, id to weekSun.plusDays(1)) }

        closer(habits, ticks, sink, startingState = StreakStateEntity(freezes = 2)).settleThrough(today)

        val byDay = sink.records.associateBy { it.date.dayOfMonth }
        assertEquals(DayVerdict.PERFECT, byDay[23]!!.verdict)
        assertEquals(DayVerdict.FROZEN, byDay[25]!!.verdict)
        assertTrue(byDay[25]!!.freezeSpent)
        assertEquals("frozen days hold the streak, they do not add to it", 2, byDay[25]!!.streakAfter)
    }

    @Test
    fun `settling twice is idempotent`() = runTest {
        val sink = Sink()
        val habits = listOf(habit(1, "Read"))
        val ticks = (0..4).map { 1L to weekSun.plusDays(it.toLong()) }
        val c = closer(habits, ticks, sink)

        c.settleThrough(today)
        val afterFirst = sink.records.size
        val stateAfterFirst = sink.state

        val closedAgain = c.settleThrough(today)

        assertEquals(0, closedAgain)
        assertEquals(afterFirst, sink.records.size)
        assertEquals(stateAfterFirst, sink.state)
    }

    @Test
    fun `changing the rules cannot rewrite a settled day`() = runTest {
        // The immutability guarantee, end to end: settle a week, then settle again with a schedule
        // that would have produced a different verdict. The stored records must not move.
        val sink = Sink()
        val lenient = listOf(habit(1, "Read", Schedule.timesPerWeek(1)))
        closer(lenient, emptyList(), sink).settleThrough(today)
        val original = sink.records.map { it.date to it.verdict }

        val strict = listOf(habit(1, "Read", Schedule.daily()))
        closer(strict, emptyList(), sink, startingState = sink.state).settleThrough(today)

        assertEquals(original, sink.records.map { it.date to it.verdict })
        assertFalse("no duplicate rows either", sink.records.map { it.date }.let { it.size != it.distinct().size })
    }
}
