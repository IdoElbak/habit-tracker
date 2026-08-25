package com.idoelbak.tracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The backup format itself. Restoring is a database job and belongs in an instrumented test, but the
 * part that can silently rot -- the file a year-old app has to still read -- is checked here.
 */
class BackupTest {

    private val sample = Backup(
        exportedAt = 1_700_000_000_000,
        habits = listOf(
            HabitDto(
                id = 1,
                name = "לקרוא 10 pages ביום",
                emoji = "📖",
                scheduleType = "DAILY",
                createdAt = 1_600_000_000_000
            )
        ),
        completions = listOf(CompletionDto(1, 20_000, 1_700_000_000_000)),
        days = listOf(DayDto(20_000, 3, 3, "PERFECT", 0, false, false, 4, 1_700_000_000_000)),
        ratings = listOf(RatingDto(20_000, mood = 7, motivation = null, notedAt = 1L)),
        streak = StreakDto(4, 9, 3, 2, 5, 20_000)
    )

    @Test
    fun `a backup survives the round trip unchanged`() {
        val text = kotlinx.serialization.json.Json.encodeToString(sample)
        val back = Backups.read(text).getOrThrow()
        assertEquals(sample, back)
        assertEquals("Hebrew names come back intact", "לקרוא 10 pages ביום", back.habits.single().name)
    }

    @Test
    fun `a backup from a newer app is refused rather than half-read`() {
        val fromTheFuture = """{"version":99,"exportedAt":1,"habits":[]}"""
        assertTrue(Backups.read(fromTheFuture).isFailure)
    }

    @Test
    fun `rubbish is a failure, not a crash`() {
        assertTrue(Backups.read("this is not json").isFailure)
        assertTrue(Backups.read("").isFailure)
    }

    @Test
    fun `an older backup missing newer fields still loads`() {
        // Only the fields version 1 had. Anything added later must default rather than throw.
        val old = """{"version":1,"exportedAt":1,"habits":[{"id":1,"name":"Read",""" +
            """"scheduleType":"DAILY","createdAt":1}]}"""
        val back = Backups.read(old).getOrThrow()
        assertEquals(1, back.habits.size)
        assertTrue(back.completions.isEmpty())
        assertEquals(BackupSummary(1, 0, 0), Backups.summarise(back))
    }
}
