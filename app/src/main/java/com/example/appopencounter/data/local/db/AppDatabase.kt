package com.example.appopencounter.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appopencounter.data.local.dao.AppInfoDao
import com.example.appopencounter.data.local.dao.DailyAppUsageDao
import com.example.appopencounter.data.local.entity.AppInfoEntity
import com.example.appopencounter.data.local.entity.DailyAppUsageEntity

@Database(
    entities = [
        AppInfoEntity::class,
        DailyAppUsageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao

    abstract fun dailyAppUsageDao(): DailyAppUsageDao

    companion object {
        private const val DATABASE_NAME = "app_open_counter.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
