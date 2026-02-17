package com.iamonzon.dory.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val DAILY_DIGEST_CHANNEL_ID = "dory_daily_digest"

    /**
     * Creates the daily digest notification channel.
     * Safe to call multiple times — Android ignores duplicate channel creation.
     * Integration stream will call this from DoryApplication.onCreate().
     */
    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            DAILY_DIGEST_CHANNEL_ID,
            "Daily Review Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily notification with due review counts"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
