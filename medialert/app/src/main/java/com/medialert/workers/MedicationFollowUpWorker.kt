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
class MedicationFollowUpWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val doseLogRepository: DoseLogRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val doseLogId = inputData.getString(AppConstants.EXTRA_DOSE_LOG_ID) ?: return Result.failure()
        val notificationId = inputData.getInt(AppConstants.EXTRA_NOTIFICATION_ID, 0)
        val isFinal = inputData.getBoolean("is_final", false)

        val doseLog = doseLogRepository.getDoseLogById(doseLogId) ?: return Result.success()

        // Only show follow-up if still pending
        if (doseLog.estado.key != "PENDIENTE") return Result.success()

        val medication = medicationRepository.getMedicationByIdOnce(doseLog.medicationId)
            ?: return Result.failure()

        val notification = notificationHelper.buildFollowUpNotification(
            doseLog = doseLog,
            medication = medication,
            notificationId = notificationId,
            isFinal = isFinal
        )
        notificationHelper.showNotification(notificationId, notification)

        // If not final, schedule the final follow-up at 15 minutes
        if (!isFinal) {
            val finalData = workDataOf(
                AppConstants.EXTRA_DOSE_LOG_ID to doseLogId,
                AppConstants.EXTRA_NOTIFICATION_ID to (notificationId + 1000),
                "is_final" to true
            )
            val finalRequest = OneTimeWorkRequestBuilder<MedicationFollowUpWorker>()
                .setInitialDelay(
                    AppConstants.FOLLOWUP_SECOND_MINUTES - AppConstants.FOLLOWUP_FIRST_MINUTES,
                    TimeUnit.MINUTES
                )
                .setInputData(finalData)
                .addTag("followup_final_$doseLogId")
                .build()

            WorkManager.getInstance(applicationContext).enqueue(finalRequest)
        }

        return Result.success()
    }
}
