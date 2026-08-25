package com.idoelbak.tracker.data

import androidx.room.withTransaction
import com.idoelbak.tracker.core.engine.DayVerdict
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.db.CompletionEntity
import com.idoelbak.tracker.data.db.DayRatingEntity
import com.idoelbak.tracker.data.db.DayRecordEntity
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.data.db.StreakStateEntity
import com.idoelbak.tracker.data.db.TrackerDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * The whole database as one file.
 *
 * Dates travel as epoch days and times as epoch millis -- the same shape they have in SQLite, so a
 * backup cannot drift a day because of a timezone. The format carries its own version, because a
 * backup taken today has to still restore in two years.
 */
@Serializable
data class Backup(
    val version: Int = FORMAT_VERSION,
    val exportedAt: Long,
    val habits: List<HabitDto> = emptyList(),
    val completions: List<CompletionDto> = emptyList(),
    val days: List<DayDto> = emptyList(),
    val ratings: List<RatingDto> = emptyList(),
    val streak: StreakDto? = null
) {
    companion object {
        const val FORMAT_VERSION = 1
    }
}

@Serializable
data class HabitDto(
    val id: Long,
    val name: String,
    val emoji: String? = null,
    val scheduleType: String,
    val timesPerWeek: Int = 0,
    val weekdayMask: Int = 0,
    val orderIndex: Int = 0,
    val reminderMinuteOfDay: Int? = null,
    val createdAt: Long,
    val archivedAt: Long? = null
)

@Serializable
data class CompletionDto(val habitId: Long, val date: Long, val completedAt: Long)

@Serializable
data class DayDto(
    val date: Long,
    val dueCount: Int,
    val doneCount: Int,
    val verdict: String,
    val allowanceApplied: Int,
    val freezeSpent: Boolean,
    val freezeEarned: Boolean,
    val streakAfter: Int,
    val closedAt: Long
)

@Serializable
data class RatingDto(val date: Long, val mood: Int? = null, val motivation: Int? = null, val notedAt: Long)

@Serializable
data class StreakDto(
    val currentStreak: Int,
    val bestStreak: Int,
    val perfectDays: Int,
    val freezes: Int,
    val cleanDays: Int,
    val lastClosedDate: Long? = null
)

/** What a restore is about to replace, so the confirmation can say it out loud. */
data class BackupSummary(val habits: Int, val completions: Int, val days: Int)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object Backups {

    suspend fun toJson(db: TrackerDatabase, now: Long): String {
        val backup = Backup(
            exportedAt = now,
            habits = db.habits().allHabits().map {
                HabitDto(
                    it.id, it.name, it.emoji, it.scheduleType.name, it.timesPerWeek, it.weekdayMask,
                    it.orderIndex, it.reminderMinuteOfDay, it.createdAt, it.archivedAt
                )
            },
            completions = db.completions().allCompletions().map {
                CompletionDto(it.habitId, it.date.toEpochDay(), it.completedAt)
            },
            days = db.dayRecords().allRecords().map {
                DayDto(
                    it.date.toEpochDay(), it.dueCount, it.doneCount, it.verdict.name,
                    it.allowanceApplied, it.freezeSpent, it.freezeEarned, it.streakAfter, it.closedAt
                )
            },
            ratings = db.dayRatings().allRatings().map {
                RatingDto(it.date.toEpochDay(), it.mood, it.motivation, it.notedAt)
            },
            streak = db.streakState().current()?.let {
                StreakDto(
                    it.currentStreak, it.bestStreak, it.perfectDays, it.freezes, it.cleanDays,
                    it.lastClosedDate?.toEpochDay()
                )
            }
        )
        return json.encodeToString(backup)
    }

    /**
     * One row per completed day per habit -- the shape the spreadsheet this app replaces had, so it
     * can be dropped straight into Sheets.
     */
    suspend fun toCsv(db: TrackerDatabase): String {
        val names = db.habits().allHabits().associate { it.id to it.name }
        return buildString {
            append("date,habit,done\n")
            db.completions().allCompletions()
                .sortedWith(compareBy({ it.date }, { it.habitId }))
                .forEach { tick ->
                    append(tick.date).append(',')
                    append(csvField(names[tick.habitId] ?: "#${tick.habitId}")).append(",1\n")
                }
        }
    }

    fun read(text: String): Result<Backup> = runCatching {
        val backup = json.decodeFromString<Backup>(text)
        require(backup.version <= Backup.FORMAT_VERSION) {
            "This backup was written by a newer version of the app."
        }
        backup
    }

    fun summarise(backup: Backup) =
        BackupSummary(backup.habits.size, backup.completions.size, backup.days.size)

    /**
     * Replaces everything. A restore is not a merge: two half-merged histories would produce streak
     * numbers that never happened. It runs in one transaction, so a failure leaves the old data
     * exactly where it was.
     */
    suspend fun restore(db: TrackerDatabase, backup: Backup) = db.withTransaction {
        db.completions().clear()
        db.dayRecords().clear()
        db.dayRatings().clear()
        db.habits().clear()

        db.habits().insertAll(
            backup.habits.map {
                HabitEntity(
                    id = it.id,
                    name = it.name,
                    emoji = it.emoji,
                    scheduleType = ScheduleType.valueOf(it.scheduleType),
                    timesPerWeek = it.timesPerWeek,
                    weekdayMask = it.weekdayMask,
                    orderIndex = it.orderIndex,
                    reminderMinuteOfDay = it.reminderMinuteOfDay,
                    createdAt = it.createdAt,
                    archivedAt = it.archivedAt
                )
            }
        )
        db.completions().insertAll(
            backup.completions.map {
                CompletionEntity(it.habitId, LocalDate.ofEpochDay(it.date), it.completedAt)
            }
        )
        db.dayRecords().insertAll(
            backup.days.map {
                DayRecordEntity(
                    date = LocalDate.ofEpochDay(it.date),
                    dueCount = it.dueCount,
                    doneCount = it.doneCount,
                    verdict = DayVerdict.valueOf(it.verdict),
                    allowanceApplied = it.allowanceApplied,
                    freezeSpent = it.freezeSpent,
                    freezeEarned = it.freezeEarned,
                    streakAfter = it.streakAfter,
                    closedAt = it.closedAt
                )
            }
        )
        db.dayRatings().insertAll(
            backup.ratings.map {
                DayRatingEntity(LocalDate.ofEpochDay(it.date), it.mood, it.motivation, it.notedAt)
            }
        )
        backup.streak?.let {
            db.streakState().put(
                StreakStateEntity(
                    currentStreak = it.currentStreak,
                    bestStreak = it.bestStreak,
                    perfectDays = it.perfectDays,
                    freezes = it.freezes,
                    cleanDays = it.cleanDays,
                    lastClosedDate = it.lastClosedDate?.let(LocalDate::ofEpochDay)
                )
            )
        }
    }

    /** A habit called `Read, daily` must not become two columns. */
    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
