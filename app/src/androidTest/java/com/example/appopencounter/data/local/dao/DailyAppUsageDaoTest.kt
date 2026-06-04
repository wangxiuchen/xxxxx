package com.example.appopencounter.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.appopencounter.data.local.db.AppDatabase
import com.example.appopencounter.data.local.entity.DailyAppUsageEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyAppUsageDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dailyAppUsageDao: DailyAppUsageDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        dailyAppUsageDao = database.dailyAppUsageDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertDailyUsage_accumulatesOpenCountWithoutDuplicatePackageDateRows() = runBlocking {
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.notes",
                date = "2026-06-04",
                openCount = 1,
                firstOpenTime = 1000L,
                lastOpenTime = 3000L,
            ),
        )
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.notes",
                date = "2026-06-04",
                openCount = 2,
                firstOpenTime = 900L,
                lastOpenTime = 5000L,
            ),
        )

        val ranking = dailyAppUsageDao.getRankingByDate("2026-06-04")
        val storedUsage = dailyAppUsageDao.getUsageByPackageAndDate(
            packageName = "com.example.notes",
            date = "2026-06-04",
        )

        assertEquals(1, ranking.size)
        assertNotNull(storedUsage)
        assertEquals(3, storedUsage?.openCount)
        assertEquals(900L, storedUsage?.firstOpenTime)
        assertEquals(5000L, storedUsage?.lastOpenTime)
    }

    @Test
    fun getRankingByDate_ordersByOpenCountDescending() = runBlocking {
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.browser",
                date = "2026-06-04",
                openCount = 2,
            ),
        )
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.mail",
                date = "2026-06-04",
                openCount = 5,
            ),
        )

        val ranking = dailyAppUsageDao.getRankingByDate("2026-06-04")

        assertEquals("com.example.mail", ranking[0].packageName)
        assertEquals("com.example.browser", ranking[1].packageName)
    }

    @Test
    fun getUsageForPackageBetweenDates_returnsRecentSevenDayRange() = runBlocking {
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.mail",
                date = "2026-05-29",
                openCount = 1,
            ),
        )
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.mail",
                date = "2026-06-04",
                openCount = 4,
            ),
        )
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.mail",
                date = "2026-05-20",
                openCount = 9,
            ),
        )

        val trend = dailyAppUsageDao.getUsageForPackageBetweenDates(
            packageName = "com.example.mail",
            fromDate = "2026-05-29",
            toDate = "2026-06-04",
        )

        assertEquals(2, trend.size)
        assertEquals("2026-05-29", trend[0].date)
        assertEquals("2026-06-04", trend[1].date)
    }

    @Test
    fun clearAll_removesDailyUsageRows() = runBlocking {
        dailyAppUsageDao.upsertDailyUsage(
            usage(
                packageName = "com.example.mail",
                date = "2026-06-04",
                openCount = 4,
            ),
        )

        dailyAppUsageDao.clearAll()

        assertNull(
            dailyAppUsageDao.getUsageByPackageAndDate(
                packageName = "com.example.mail",
                date = "2026-06-04",
            ),
        )
    }

    private fun usage(
        packageName: String,
        date: String,
        openCount: Int,
        firstOpenTime: Long? = 1000L,
        lastOpenTime: Long? = 3000L,
        updatedAt: Long = 6000L,
    ): DailyAppUsageEntity {
        return DailyAppUsageEntity(
            packageName = packageName,
            date = date,
            openCount = openCount,
            firstOpenTime = firstOpenTime,
            lastOpenTime = lastOpenTime,
            updatedAt = updatedAt,
        )
    }
}
