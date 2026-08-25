package com.idoelbak.tracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archivedAt IS NULL ORDER BY orderIndex, id")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY orderIndex, id")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archivedAt IS NULL ORDER BY orderIndex, id")
    suspend fun activeHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: Long): HabitEntity?

    /**
     * The habits that existed on a past day -- archived ones included, as long as they were still
     * active then. Settling last Tuesday has to judge it by the habits that were live on Tuesday.
     */
    @Query(
        "SELECT * FROM habits WHERE createdAt <= :at AND (archivedAt IS NULL OR archivedAt >= :at) " +
            "ORDER BY orderIndex, id"
    )
    suspend fun activeOn(at: Long): List<HabitEntity>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM habits")
    suspend fun nextOrderIndex(): Int

    @Query("SELECT * FROM habits ORDER BY id")
    suspend fun allHabits(): List<HabitEntity>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Insert
    suspend fun insertAll(habits: List<HabitEntity>)

    /** Restore only. Everything else archives. */
    @Query("DELETE FROM habits")
    suspend fun clear()

    @Update
    suspend fun update(habit: HabitEntity)

    /** Archiving rather than deleting keeps this habit's history in every past percentage. */
    @Query("UPDATE habits SET archivedAt = :at WHERE id = :id")
    suspend fun archive(id: Long, at: Long)

    @Query("UPDATE habits SET archivedAt = NULL WHERE id = :id")
    suspend fun unarchive(id: Long)

    /** Only for a habit created by mistake -- this drops its completions too. */
    @Delete
    suspend fun deleteForever(habit: HabitEntity)
}

@Dao
interface CompletionDao {

    @Query("SELECT * FROM completions WHERE date = :date")
    fun observeOn(date: LocalDate): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun between(from: LocalDate, to: LocalDate): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId ORDER BY date")
    suspend fun forHabit(habitId: Long): List<CompletionEntity>

    @Query("SELECT COUNT(*) FROM completions WHERE habitId = :habitId AND date BETWEEN :from AND :to")
    suspend fun countForHabitBetween(habitId: Long, from: LocalDate, to: LocalDate): Int

    @Query("SELECT COUNT(*) FROM completions WHERE date = :date")
    suspend fun countOn(date: LocalDate): Int

    @Query("SELECT EXISTS(SELECT 1 FROM completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isDone(habitId: Long, date: LocalDate): Boolean

    @Query("SELECT * FROM completions")
    suspend fun allCompletions(): List<CompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun tick(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(completions: List<CompletionEntity>)

    @Query("DELETE FROM completions")
    suspend fun clear()

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun untick(habitId: Long, date: LocalDate)
}

@Dao
interface DayRecordDao {

    @Query("SELECT * FROM day_records WHERE date = :date")
    suspend fun on(date: LocalDate): DayRecordEntity?

    @Query("SELECT * FROM day_records WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM day_records WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun between(from: LocalDate, to: LocalDate): List<DayRecordEntity>

    @Query("SELECT MAX(date) FROM day_records")
    suspend fun latestClosedDate(): LocalDate?

    @Query("SELECT * FROM day_records ORDER BY date")
    suspend fun allRecords(): List<DayRecordEntity>

    /**
     * Insert only -- a closed day is never rewritten. IGNORE rather than REPLACE is the mechanism
     * that makes past verdicts immutable.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun close(record: DayRecordEntity): Long

    /** Restore only -- the immutability rule protects a live database, not a wiped one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DayRecordEntity>)

    @Query("DELETE FROM day_records")
    suspend fun clear()
}

@Dao
interface DayRatingDao {

    @Query("SELECT * FROM day_ratings WHERE date = :date")
    fun observeOn(date: LocalDate): Flow<DayRatingEntity?>

    @Query("SELECT * FROM day_ratings WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun between(from: LocalDate, to: LocalDate): List<DayRatingEntity>

    @Query("SELECT * FROM day_ratings WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DayRatingEntity>>

    @Query("SELECT * FROM day_ratings ORDER BY date")
    suspend fun allRatings(): List<DayRatingEntity>

    @Upsert
    suspend fun put(rating: DayRatingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ratings: List<DayRatingEntity>)

    @Query("DELETE FROM day_ratings")
    suspend fun clear()
}

@Dao
interface StreakStateDao {

    @Query("SELECT * FROM streak_state WHERE id = ${StreakStateEntity.SINGLETON_ID}")
    fun observe(): Flow<StreakStateEntity?>

    @Query("SELECT * FROM streak_state WHERE id = ${StreakStateEntity.SINGLETON_ID}")
    suspend fun current(): StreakStateEntity?

    @Upsert
    suspend fun put(state: StreakStateEntity)
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY orderIndex, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Delete
    suspend fun delete(category: CategoryEntity)
}
