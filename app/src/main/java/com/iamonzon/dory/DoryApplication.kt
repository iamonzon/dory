package com.iamonzon.dory

import android.app.Application
import com.iamonzon.dory.notifications.DailyDigestScheduler
import com.iamonzon.dory.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoryApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.createChannels(this)

        CoroutineScope(Dispatchers.IO).launch {
            val hour = container.settingsRepository.getNotificationHour()
            val minute = container.settingsRepository.getNotificationMinute()
            DailyDigestScheduler.schedule(this@DoryApplication, hour, minute)
        }
    }
}
