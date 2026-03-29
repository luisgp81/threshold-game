package com.medialert.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.DoseLog
import com.medialert.utils.AppConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.ScheduleRepository
import com.medialert.workers.MedicationAlarmWorker

/**
 * Receives exact alarms fired by AlarmManager for medication reminders.
 * Creates the DoseLog entry and hands off to MedicationAlarmWorker for notification.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var doseLogRepository: DoseLogRepository
    @Inject lateinit var scheduleRepository: ScheduleRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppConstants.ACTION_MEDICATION_ALARM) return

        val medicationId = intent.getStringExtra(AppConstants.EXTRA_MEDICATION_ID) ?: return
        val scheduleId = intent.getStringExtra("schedule_id") ?: return
        val triggerTime = intent.getLongExtra("trigger_time", System.currentTimeMillis())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create DoseLog entry for this alarm
                val doseLogId = UUID.randomUUID().toString()
                val doseLog = DoseLog(
                    id = doseLogId,
                    medicationId = medicationId,
                    scheduleId = scheduleId,
                    fechaProgramada = triggerTime,
                    fechaTomada = null,
                    estado = DoseEstado.PENDIENTE,
                    fotoPath = null,
                    notas = null
                )
                doseLogRepository.saveDoseLog(doseLog)

                val notificationId = AppConstants.NOTIF_ID_REMINDER_BASE + doseLogId.hashCode()

                // Trigger notification via WorkManager for reliability
                val data = Data.Builder()
                    .putString(AppConstants.EXTRA_DOSE_LOG_ID, doseLogId)
                    .putInt(AppConstants.EXTRA_NOTIFICATION_ID, notificationId)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<MedicationAlarmWorker>()
                    .setInputData(data)
                    .addTag("alarm_$doseLogId")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
