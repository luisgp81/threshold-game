package com.medialert.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.notifications.NotificationHelper
import com.medialert.utils.AppConstants
import com.medialert.workers.MedicationFollowUpWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Handles taps on notification action buttons:
 * ✅ Taken, ⏰ Snooze (5 min), ❌ Skip.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var doseLogRepository: DoseLogRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getStringExtra(AppConstants.EXTRA_DOSE_LOG_ID) ?: return
        val notificationId = intent.getIntExtra(AppConstants.EXTRA_NOTIFICATION_ID, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AppConstants.ACTION_DOSE_TAKEN -> {
                        doseLogRepository.updateDoseStatus(doseLogId, DoseEstado.TOMADA, System.currentTimeMillis())
                        notificationHelper.cancelNotification(notificationId)
                        // Cancel follow-up workers
                        WorkManager.getInstance(context).cancelAllWorkByTag("followup_$doseLogId")
                        WorkManager.getInstance(context).cancelAllWorkByTag("followup_final_$doseLogId")
                    }
                    AppConstants.ACTION_DOSE_SNOOZE -> {
                        notificationHelper.cancelNotification(notificationId)
                        // Re-show reminder after 5 minutes
                        val data = workDataOf(
                            AppConstants.EXTRA_DOSE_LOG_ID to doseLogId,
                            AppConstants.EXTRA_NOTIFICATION_ID to notificationId
                        )
                        val snoozeWork = OneTimeWorkRequestBuilder<MedicationFollowUpWorker>()
                            .setInitialDelay(AppConstants.SNOOZE_MINUTES, TimeUnit.MINUTES)
                            .setInputData(data)
                            .addTag("snooze_$doseLogId")
                            .build()
                        WorkManager.getInstance(context).enqueue(snoozeWork)
                    }
                    AppConstants.ACTION_DOSE_SKIPPED -> {
                        doseLogRepository.updateDoseStatus(doseLogId, DoseEstado.OMITIDA, null)
                        notificationHelper.cancelNotification(notificationId)
                        WorkManager.getInstance(context).cancelAllWorkByTag("followup_$doseLogId")
                        WorkManager.getInstance(context).cancelAllWorkByTag("followup_final_$doseLogId")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
