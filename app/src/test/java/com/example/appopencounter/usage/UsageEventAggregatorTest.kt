package com.example.appopencounter.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class UsageEventAggregatorTest {
    private val zoneId = ZoneId.of("UTC")
    private val aggregator = UsageEventAggregator(
        currentPackageName = CURRENT_PACKAGE,
        zoneId = zoneId,
    )

    @Test
    fun aggregate_countsNormalOpenWhenForegroundDurationIsLongerThanTwoSeconds() {
        val events = aggregator.aggregate(
            events = listOf(
                foreground("com.example.mail", at("2026-06-04T10:00:00")),
                background("com.example.mail", at("2026-06-04T10:00:03")),
            ),
            rangeEndTimeMillis = at("2026-06-04T10:00:05"),
        )

        assertEquals(1, events.size)
        assertEquals("com.example.mail", events.single().packageName)
        assertEquals("2026-06-04", events.single().date)
        assertEquals(3_000L, events.single().durationMs)
    }

    @Test
    fun aggregate_ignoresAccidentalOpenShorterThanTwoSeconds() {
        val events = aggregator.aggregate(
            events = listOf(
                foreground("com.example.mail", at("2026-06-04T10:00:00")),
                background("com.example.mail", at("2026-06-04T10:00:01")),
            ),
            rangeEndTimeMillis = at("2026-06-04T10:00:05"),
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun aggregate_doesNotDoubleCountDuplicateForegroundEventsInSameSession() {
        val events = aggregator.aggregate(
            events = listOf(
                foreground("com.example.mail", at("2026-06-04T10:00:00")),
                foreground("com.example.mail", at("2026-06-04T10:00:01")),
                background("com.example.mail", at("2026-06-04T10:00:04")),
            ),
            rangeEndTimeMillis = at("2026-06-04T10:00:05"),
        )

        assertEquals(1, events.size)
        assertEquals(4_000L, events.single().durationMs)
    }

    @Test
    fun aggregate_countsMultipleAppsWhenSwitchingForegroundApps() {
        val events = aggregator.aggregate(
            events = listOf(
                foreground("com.example.mail", at("2026-06-04T10:00:00")),
                foreground("com.example.browser", at("2026-06-04T10:00:03")),
                background("com.example.browser", at("2026-06-04T10:00:07")),
            ),
            rangeEndTimeMillis = at("2026-06-04T10:00:08"),
        )

        assertEquals(2, events.size)
        assertEquals("com.example.mail", events[0].packageName)
        assertEquals("com.example.browser", events[1].packageName)
    }

    @Test
    fun aggregate_splitsCrossDaySessionIntoDailyRecords() {
        val events = aggregator.aggregate(
            events = listOf(
                foreground("com.example.reader", at("2026-06-04T23:59:57")),
                background("com.example.reader", at("2026-06-05T00:00:03")),
            ),
            rangeEndTimeMillis = at("2026-06-05T00:00:04"),
        )

        assertEquals(2, events.size)
        assertEquals("2026-06-04", events[0].date)
        assertEquals("2026-06-05", events[1].date)
        assertEquals(3_000L, events[0].durationMs)
        assertEquals(3_000L, events[1].durationMs)
    }

    @Test
    fun aggregate_excludesCurrentAppItself() {
        val events = aggregator.aggregate(
            events = listOf(
                foreground(CURRENT_PACKAGE, at("2026-06-04T10:00:00")),
                background(CURRENT_PACKAGE, at("2026-06-04T10:00:10")),
            ),
            rangeEndTimeMillis = at("2026-06-04T10:00:12"),
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun aggregateDailyUsage_groupsOpenEventsByPackageAndDate() {
        val dailyUsage = aggregator.aggregateDailyUsage(
            events = listOf(
                foreground("com.example.mail", at("2026-06-04T10:00:00")),
                background("com.example.mail", at("2026-06-04T10:00:03")),
                foreground("com.example.mail", at("2026-06-04T11:00:00")),
                background("com.example.mail", at("2026-06-04T11:00:03")),
            ),
            rangeEndTimeMillis = at("2026-06-04T11:00:05"),
            updatedAt = at("2026-06-04T12:00:00"),
        )

        assertEquals(1, dailyUsage.size)
        assertEquals("com.example.mail", dailyUsage.single().packageName)
        assertEquals("2026-06-04", dailyUsage.single().date)
        assertEquals(2, dailyUsage.single().openCount)
    }

    private fun foreground(
        packageName: String,
        timestamp: Long,
    ) = UsageEventRecord(
        packageName = packageName,
        timestamp = timestamp,
        kind = UsageEventKind.FOREGROUND,
    )

    private fun background(
        packageName: String,
        timestamp: Long,
    ) = UsageEventRecord(
        packageName = packageName,
        timestamp = timestamp,
        kind = UsageEventKind.BACKGROUND,
    )

    private fun at(value: String): Long {
        val parts = value.split("T")
        val date = LocalDate.parse(parts[0])
        val time = LocalTime.parse(parts[1])
        return LocalDateTime.of(date, time)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private companion object {
        const val CURRENT_PACKAGE = "com.example.appopencounter"
    }
}
