package com.idoelbak.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryEntity::class,
        HabitEntity::class,
        CompletionEntity::class,
        DayRecordEntity::class,
        DayRatingEntity::class,
        StreakStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TrackerDatabase : RoomDatabase() {

    abstract fun habits(): HabitDao
    abstract fun completions(): CompletionDao
    abstract fun dayRecords(): DayRecordDao
    abstract fun dayRatings(): DayRatingDao
    abstract fun streakState(): StreakStateDao
    abstract fun categories(): CategoryDao

    companion object {
        private const val NAME = "tracker.db"

        @Volatile
        private var instance: TrackerDatabase? = null

        fun get(context: Context): TrackerDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): TrackerDatabase =
            Room.databaseBuilder(context, TrackerDatabase::class.java, NAME)
                // Room turns on foreign-key enforcement itself, so archiving a habit cannot leave
                // orphaned completions behind. No fallbackToDestructiveMigration: losing a year of
                // history to a schema bump is not an acceptable failure mode.
                .build()
    }
}
