package com.yourname.habitapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourname.habitapp.data.dao.*
import com.yourname.habitapp.data.models.*

// ─── Type Converters لتحويل Enum و Long? ────────────────────────────────────

class Converters {
    @androidx.room.TypeConverter
    fun fromHabitFrequency(value: HabitFrequency): String = value.name

    @androidx.room.TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)

    @androidx.room.TypeConverter
    fun fromHabitCategory(value: HabitCategory): String = value.name

    @androidx.room.TypeConverter
    fun toHabitCategory(value: String): HabitCategory = try {
        HabitCategory.valueOf(value)
    } catch (e: Exception) {
        when(value) {
            "WORK" -> HabitCategory.PRODUCTIVITY
            "SPORT" -> HabitCategory.FITNESS
            else -> HabitCategory.OTHER
        }
    }

    @androidx.room.TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @androidx.room.TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)

    @androidx.room.TypeConverter
    fun fromTaskType(value: TaskType): String = value.name

    @androidx.room.TypeConverter
    fun toTaskType(value: String): TaskType = TaskType.valueOf(value)

    @androidx.room.TypeConverter
    fun fromGoalFrequency(value: GoalFrequency): String = value.name

    @androidx.room.TypeConverter
    fun toGoalFrequency(value: String): GoalFrequency = GoalFrequency.valueOf(value)
}

// ─── AppDatabase ─────────────────────────────────────────────────────────────

@Database(
    entities = [
        Habit::class,
        TodoItem::class,
        YearGoal::class,
        GoalStep::class,
        Achievement::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
    abstract fun yearGoalDao(): YearGoalDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton Pattern — نسخة واحدة فقط في الذاكرة
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_app_database"
                )
                .fallbackToDestructiveMigration()   // يحذف ويعيد عند تغيير الهيكل
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
