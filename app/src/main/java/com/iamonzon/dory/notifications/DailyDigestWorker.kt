package com.iamonzon.dory.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iamonzon.dory.MainActivity
import com.iamonzon.dory.R
import com.iamonzon.dory.data.db.DoryDatabase
import com.iamonzon.dory.data.model.ReviewUrgency
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import com.iamonzon.dory.data.repository.dataStore
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that checks for due/overdue items and shows a notification.
 * Skips the notification if nothing is due.
 */
class DailyDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = DoryDatabase.create(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext.dataStore)
        val itemRepository = ItemRepository(
            itemDao = database.itemDao(),
            reviewDao = database.reviewDao(),
            categoryDao = database.categoryDao(),
            settingsRepository = settingsRepository
        )

        val dueItems = itemRepository.observeDueItems().first()
        if (dueItems.isEmpty()) {
            return Result.success()
        }

        val overdueCount = dueItems.count { it.urgency == ReviewUrgency.Overdue }
        val dueTodayCount = dueItems.count { it.urgency == ReviewUrgency.DueToday }

        val body = when {
            overdueCount > 0 && dueTodayCount > 0 ->
                "$overdueCount items overdue, $dueTodayCount due today"
            overdueCount > 0 ->
                "$overdueCount items overdue"
            else ->
                "$dueTodayCount items due for review"
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.DAILY_DIGEST_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Dory Review Reminder")
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS permission — nothing we can do here
        }

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
