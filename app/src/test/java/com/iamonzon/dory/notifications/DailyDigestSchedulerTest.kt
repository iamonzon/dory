package com.iamonzon.dory.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class DailyDigestSchedulerTest {

    @Test
    fun `delay is positive when target time is later today`() {
        val now = LocalDateTime.of(2025, 1, 15, 8, 0)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
        assertEquals(Duration.ofHours(1), delay)
    }

    @Test
    fun `delay rolls to tomorrow when target time has passed`() {
        val now = LocalDateTime.of(2025, 1, 15, 10, 0)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
        assertEquals(Duration.ofHours(23), delay)
    }

    @Test
    fun `delay rolls to tomorrow when target is exactly now`() {
        val now = LocalDateTime.of(2025, 1, 15, 9, 0)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
        assertEquals(Duration.ofHours(24), delay)
    }

    @Test
    fun `delay handles minutes correctly`() {
        val now = LocalDateTime.of(2025, 1, 15, 8, 30)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 15)
        assertEquals(Duration.ofMinutes(45), delay)
    }

    @Test
    fun `delay near midnight rolls correctly`() {
        val now = LocalDateTime.of(2025, 1, 15, 23, 59)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 6, 0)
        assertEquals(Duration.ofMinutes(361), delay) // 6h 1m
    }

    @Test
    fun `delay is always non-negative`() {
        // Test various times throughout the day
        val times = listOf(0, 3, 6, 9, 12, 15, 18, 21, 23)
        for (hour in times) {
            val now = LocalDateTime.of(2025, 6, 1, hour, 30)
            val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
            assertTrue("Delay should be non-negative for hour=$hour", !delay.isNegative)
        }
    }

    @Test
    fun `delay is at most 24 hours`() {
        val times = listOf(0, 3, 6, 9, 12, 15, 18, 21, 23)
        for (hour in times) {
            val now = LocalDateTime.of(2025, 6, 1, hour, 30)
            val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
            assertTrue(
                "Delay should be <= 24h for hour=$hour, was ${delay.toMinutes()}m",
                delay <= Duration.ofHours(24)
            )
        }
    }

    @Test
    fun `delay with target at midnight`() {
        val now = LocalDateTime.of(2025, 1, 15, 22, 0)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 0, 0)
        assertEquals(Duration.ofHours(2), delay)
    }

    @Test
    fun `delay one minute before target`() {
        val now = LocalDateTime.of(2025, 1, 15, 8, 59)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
        assertEquals(Duration.ofMinutes(1), delay)
    }

    @Test
    fun `delay one minute after target rolls to tomorrow`() {
        val now = LocalDateTime.of(2025, 1, 15, 9, 1)
        val delay = DailyDigestScheduler.computeInitialDelay(now, 9, 0)
        assertEquals(Duration.ofHours(24).minusMinutes(1), delay)
    }
}
