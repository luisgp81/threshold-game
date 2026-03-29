package com.medialert.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.medialert.workers.AlarmSchedulerWorker
import com.medialert.workers.DailyDoseGeneratorWorker

/**
 * Reprograms all active alarms after device reboot.
 * Also triggers a daily dose generation to cover any missed midnight run.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val workManager = WorkManager.getInstance(context)

        // Re-generate today's doses (in case midnight worker missed)
        val generateRequest = OneTimeWorkRequestBuilder<DailyDoseGeneratorWorker>().build()
        workManager.enqueue(generateRequest)

        // Re-schedule all alarms
        val scheduleRequest = OneTimeWorkRequestBuilder<AlarmSchedulerWorker>().build()
        workManager.enqueue(scheduleRequest)

        // Re-enqueue periodic daily generator
        workManager.enqueueUniquePeriodicWork(
            "daily_dose_generator",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            DailyDoseGeneratorWorker.buildPeriodicRequest()
        )
    }
}
