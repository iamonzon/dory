# Stream 4: Notifications

## Branch
`stream/notifications`

## Goal
Implement the daily digest notification system using WorkManager. A periodic worker queries due/overdue item counts and shows a notification at the user-configured time.

## Critical Constraint
**DO NOT modify any existing files.** Only create new files listed below. The notification channel setup in `DoryApplication.kt` and the WorkManager scheduling trigger in the Profile screen will be handled by Stream 6 (Integration).

## Files to Create

```
app/src/main/java/com/iamonzon/dory/notifications/NotificationChannels.kt
app/src/main/java/com/iamonzon/dory/notifications/DailyDigestWorker.kt
app/src/main/java/com/iamonzon/dory/notifications/DailyDigestScheduler.kt
app/src/test/java/com/iamonzon/dory/notifications/DailyDigestSchedulerTest.kt
```

## Component Contracts

### NotificationChannels

```kotlin
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
    fun createChannels(context: Context)
}
```

**Implementation notes:**
- Channel name: "Daily Review Reminder"
- Channel description: "Daily notification with due review counts"
- Importance: `NotificationManager.IMPORTANCE_DEFAULT`

### DailyDigestWorker

```kotlin
package com.iamonzon.dory.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker that checks for due/overdue items and shows a notification.
 * Skips the notification if nothing is due.
 */
class DailyDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result
}
```

**Implementation notes:**
- Get the database instance via `DoryDatabase.create(applicationContext)` (it's a singleton)
- Query `ItemDao.observeAllActive()` — but since this is a one-shot worker, use a direct query. You'll need to call `itemDao.getAllActive()` if it exists, or use `.first()` on the Flow.
  - **Important:** The current `ItemDao` only has `observeAllActive()` which returns `Flow`. Use `Flow.first()` to get a one-shot snapshot in the worker. Do NOT add new methods to `ItemDao`.
- Compute urgency for each item using the same logic as `ItemRepository.computeUrgency()` — replicate the urgency computation in the worker, or access it through `ItemRepository`
  - **Preferred approach:** Instantiate `ItemRepository` in the worker (using the DAOs from the database and a `SettingsRepository` from the context's DataStore) and use `observeDueItems().first()` to get due items count
- Build the notification:
  - Title: "Dory Review Reminder"
  - Body: "{N} items due for review" (or "{N} items overdue, {M} due today" if both exist)
  - If no items are due, return `Result.success()` without showing a notification
  - Set the notification's content intent to open `MainActivity` (so tapping opens the dashboard)
  - Use `NotificationChannels.DAILY_DIGEST_CHANNEL_ID`
- Return `Result.success()` always (even if notification display fails — no retry needed)

### DailyDigestScheduler

```kotlin
package com.iamonzon.dory.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager

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
    fun schedule(context: Context, hour: Int, minute: Int)

    /**
     * Cancel the daily digest worker.
     */
    fun cancel(context: Context)
}
```

**Implementation notes:**
- Use `PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)`
- Calculate initial delay: compute time until next occurrence of `hour:minute` today (or tomorrow if it's already past)
- Use `ExistingPeriodicWorkPolicy.UPDATE` so calling schedule again replaces the existing work
- The unique work name is `"dory_daily_digest"`

## Dependency Note

This stream needs WorkManager. Check that `build.gradle.kts` already includes `androidx.work:work-runtime-ktx`. If not, **do NOT modify build.gradle.kts** — instead, note it in a `STREAM-4-DEPS.md` file that the integration stream picks up.

Actually, check:
- The `gradle/libs.versions.toml` and `app/build.gradle.kts` — if WorkManager is not already listed, create a file `STREAM-4-DEPS.md` in the stream's created files listing the needed dependency:
  ```
  # Stream 4: Dependencies to Add
  ## app/build.gradle.kts
  implementation(libs.androidx.work.runtime.ktx)

  ## gradle/libs.versions.toml (if not present)
  [versions]
  work = "2.10.0"
  [libraries]
  androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
  ```

## Existing Code Reference

These are accessed from the worker (do NOT modify):

- `DoryDatabase.create(context)` — in `data/db/DoryDatabase.kt`
- `ItemRepository` constructor — in `data/repository/ItemRepository.kt`
- `ItemRepository.observeDueItems(): Flow<List<DashboardItem>>` — use `.first()` for one-shot
- `SettingsRepository(context.dataStore)` — in `data/repository/SettingsRepository.kt`
- `ReviewUrgency` enum — in `data/model/ReviewUrgency.kt`
- `DashboardItem` data class — in `data/repository/ItemRepository.kt`

## Testing Requirements

**DailyDigestSchedulerTest:**
- Test that `schedule()` computes correct initial delay for various current times
- Test that calling `schedule()` twice replaces (doesn't duplicate) the work
- Use `WorkManagerTestInitHelper` for instrumented tests, or test the delay calculation logic in a unit test

Note: `DailyDigestWorker` is harder to unit test due to Android dependencies. Verify it manually or with a minimal instrumented test. Focus unit tests on the scheduler's delay calculation.

## Acceptance Criteria

1. All notification files compile independently
2. Unit tests pass for the scheduler
3. `NotificationChannels.createChannels()` is callable from Application.onCreate()
4. `DailyDigestScheduler.schedule()` is callable with hour/minute params
5. `DailyDigestWorker` queries real data and builds a notification
6. No existing files were modified
7. If WorkManager dependency was missing, a `STREAM-4-DEPS.md` file documents what to add
