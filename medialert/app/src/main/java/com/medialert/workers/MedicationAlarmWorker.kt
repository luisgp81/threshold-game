package com.medialert.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.MedicationRepository
import com.medialert.notifications.NotificationHelper
import com.medialert.utils.AppConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MedicationAlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val doseLogId = inputData.getString(AppConstants.EXTRA_DOSE_LOG_ID)
            ?: return Result.failure()
        val notificationId = inputData.getInt(AppConstants.EXTRA_NOTIFICATION_ID, 0)

        val doseLog = doseLogRepository.getDoseLogById(doseLogId)
            ?: return Result.failure()

        // Skip if already handled
        if (doseLog.estado.key != "PENDIENTE") return Result.success()

        val medication = medicationRepository.getMedicationByIdOnce(doseLog.medicationId)
            ?: return Result.failure()

        val notification = notificationHelper.buildMedicationReminderNotification(
            doseLog = doseLog,
            medication = medication,
            notificationId = notificationId
        )
        notificationHelper.showNotification(notificationId, notification)

        // Schedule follow-up after 5 minutes
        scheduleFollowUp(
            doseLogId = doseLogId,
            notificationId = notificationId + AppConstants.NOTIF_ID_FOLLOWUP_BASE,
            delayMinutes = AppConstants.FOLLOWUP_FIRST_MINUTES,
            isFinal = false
        )

        return Result.success()
    }

    private fun scheduleFollowUp(
        doseLogId: String,
        notificationId: Int,
        delayMinutes: Long,
        isFinal: Boolean
    ) {
        val data = workDataOf(
            AppConstants.EXTRA_DOSE_LOG_ID to doseLogId,
            AppConstants.EXTRA_NOTIFICATION_ID to notificationId,
            "is_final" to isFinal
        )
        val request = OneTimeWorkRequestBuilder<MedicationFollowUpWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("followup_$doseLogId")
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
    }
}
