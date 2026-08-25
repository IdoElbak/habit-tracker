package com.idoelbak.tracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.idoelbak.tracker.core.engine.DayVerdict
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.core.model.ScheduleType
import java.time.LocalDate

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("archivedAt")]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Optional single emoji. Null means the row shows text only. */
    val emoji: String? = null,
    val categoryId: Long? = null,
    val scheduleType: ScheduleType,
    val timesPerWeek: Int = 0,
    val weekdayMask: Int = 0,
    val orderIndex: Int = 0,
    /** Minutes past midnight for this habit's own reminder; null means it uses only the daily nudges. */
    val reminderMinuteOfDay: Int? = null,
    val createdAt: Long,
    /** Set instead of deleting, so past ticks and percentages survive. */
    val archivedAt: Long? = null
) {
    val schedule: Schedule
        get() = Schedule(scheduleType, timesPerWeek, weekdayMask)

    val isActive: Boolean get() = archivedAt == null
}

@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class CompletionEntity(
    val habitId: Long,
    val date: LocalDate,
    val completedAt: Long
)

/**
 * One row per finished day, written once at close and never recomputed.
 *
 * This is the guarantee that changing a rule later cannot reach back and damage a streak that has
 * already been earned.
 */
@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val date: LocalDate,
    val dueCount: Int,
    val doneCount: Int,
    val verdict: DayVerdict,
    val allowanceApplied: Int,
    val freezeSpent: Boolean,
    val freezeEarned: Boolean,
    val streakAfter: Int,
    val closedAt: Long
)

/** How the day felt. Both values are optional -- a skipped rating is not a zero. */
@Entity(tableName = "day_ratings")
data class DayRatingEntity(
    @PrimaryKey val date: LocalDate,
    val mood: Int? = null,
    val motivation: Int? = null,
    val notedAt: Long
)

/** Single-row table holding the live streak counters. */
@Entity(tableName = "streak_state")
data class StreakStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val perfectDays: Int = 0,
    val freezes: Int = 2,
    val cleanDays: Int = 0,
    val lastClosedDate: LocalDate? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
