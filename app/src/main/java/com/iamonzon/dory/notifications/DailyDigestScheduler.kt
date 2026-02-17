package com.iamonzon.dory.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object DailyDigestScheduler {

    private const val WORK_NAME = "dory_daily_digest"

    /**
     * Schedule (or reschedule) the daily digest worker at the given hour:minute.
     * Uses PeriodicWorkRequest with 24-hour interval.
     * Calculates the initial delay so the first execution fires at the target time.
     *
     * Integration stream calls this from:
     * 1. DoryApplication.onCreate() (on app start, with saved notification time)
     * 2. ProfileViewModel.setNotificationTime() (when user changes the time)
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val initialDelay = computeInitialDelay(LocalDateTime.now(), hour, minute)

        val workRequest = PeriodicWorkRequestBuilder<DailyDigestWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Cancel the daily digest worker.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Compute the delay from [now] until the next occurrence of [hour]:[minute].
     * If the target time is in the past today, it rolls over to tomorrow.
     */
    internal fun computeInitialDelay(now: LocalDateTime, hour: Int, minute: Int): Duration {
        val targetTime = LocalTime.of(hour, minute)
        val todayTarget = now.toLocalDate().atTime(targetTime)

        val target = if (todayTarget.isAfter(now)) todayTarget
        else todayTarget.plusDays(1)

        return Duration.between(now, target)
    }
}
