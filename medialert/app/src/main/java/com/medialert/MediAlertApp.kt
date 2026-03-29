package com.medialert

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.google.android.gms.ads.MobileAds
import com.medialert.notifications.NotificationHelper
import com.medialert.workers.DailyDoseGeneratorWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MediAlertApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Create notification channels (required before posting any notification)
        notificationHelper.createChannels()

        // Enqueue daily dose generator (idempotent — KEEP policy)
        CoroutineScope(Dispatchers.Default).launch {
            WorkManager.getInstance(this@MediAlertApp).enqueueUniquePeriodicWork(
                "daily_dose_generator",
                ExistingPeriodicWorkPolicy.KEEP,
                DailyDoseGeneratorWorker.buildPeriodicRequest()
            )
        }

        // Initialize AdMob SDK (safe to call even when ads are disabled)
        if (BuildConfig.ADS_ENABLED) {
            MobileAds.initialize(this)
        }
    }
}
