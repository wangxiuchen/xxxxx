package com.example.appopencounter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.appopencounter.data.local.entity.DailyAppUsageEntity

@Dao
interface DailyAppUsageDao {
    @Transaction
    suspend fun upsertDailyUsage(usage: DailyAppUsageEntity) {
        val updatedRows = incrementExistingUsage(
            packageName = usage.packageName,
            date = usage.date,
            openCount = usage.openCount,
            firstOpenTime = usage.firstOpenTime,
            lastOpenTime = usage.lastOpenTime,
            updatedAt = usage.updatedAt,
        )

        if (updatedRows == 0) {
            val insertedId = insertIgnoringConflict(usage)
            if (insertedId == -1L) {
                incrementExistingUsage(
                    packageName = usage.packageName,
                    date = usage.date,
                    openCount = usage.openCount,
                    firstOpenTime = usage.firstOpenTime,
                    lastOpenTime = usage.lastOpenTime,
                    updatedAt = usage.updatedAt,
                )
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringConflict(usage: DailyAppUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<DailyAppUsageEntity>)

    @Transaction
    suspend fun replaceUsageForDates(
        dates: List<String>,
        usages: List<DailyAppUsageEntity>,
    ) {
        if (dates.isEmpty()) {
            return
        }
        clearByDates(dates)
        insertAll(usages)
    }

    @Query(
        """
        UPDATE daily_app_usage
        SET
            openCount = openCount + :openCount,
            firstOpenTime = CASE
                WHEN firstOpenTime IS NULL THEN :firstOpenTime
                WHEN :firstOpenTime IS NULL THEN firstOpenTime
                WHEN firstOpenTime <= :firstOpenTime THEN firstOpenTime
                ELSE :firstOpenTime
            END,
            lastOpenTime = CASE
                WHEN lastOpenTime IS NULL THEN :lastOpenTime
                WHEN :lastOpenTime IS NULL THEN lastOpenTime
                WHEN lastOpenTime >= :lastOpenTime THEN lastOpenTime
                ELSE :lastOpenTime
            END,
            updatedAt = :updatedAt
        WHERE packageName = :packageName AND date = :date
        """,
    )
    suspend fun incrementExistingUsage(
        packageName: String,
        date: String,
        openCount: Int,
        firstOpenTime: Long?,
        lastOpenTime: Long?,
        updatedAt: Long,
    ): Int

    @Query(
        """
        SELECT *
        FROM daily_app_usage
        WHERE date = :date
        ORDER BY openCount DESC, lastOpenTime DESC
        """,
    )
    suspend fun getRankingByDate(date: String): List<DailyAppUsageEntity>

    @Query(
        """
        SELECT *
        FROM daily_app_usage
        WHERE packageName = :packageName AND date BETWEEN :fromDate AND :toDate
        ORDER BY date ASC
        """,
    )
    suspend fun getUsageForPackageBetweenDates(
        packageName: String,
        fromDate: String,
        toDate: String,
    ): List<DailyAppUsageEntity>

    @Query("SELECT * FROM daily_app_usage WHERE packageName = :packageName AND date = :date")
    suspend fun getUsageByPackageAndDate(
        packageName: String,
        date: String,
    ): DailyAppUsageEntity?

    @Query("DELETE FROM daily_app_usage")
    suspend fun clearAll()

    @Query("DELETE FROM daily_app_usage WHERE date IN (:dates)")
    suspend fun clearByDates(dates: List<String>)
}
