package com.medialert.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.medialert.domain.model.Schedule
import com.medialert.domain.model.ScheduleTipo
import com.medialert.domain.repository.MedicationRepository
import com.medialert.domain.repository.ScheduleRepository
import com.medialert.receivers.AlarmReceiver
import com.medialert.utils.AppConstants
import com.medialert.utils.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Schedules AlarmManager exact alarms for all active medication schedules.
 * Called on boot and when schedules are created/updated.
 */
@HiltWorker
class AlarmSchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
    private val medicationRepository: MedicationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val schedules = scheduleRepository.getAllActiveSchedules()
        schedules.forEach { schedule ->
            val medication = medicationRepository.getMedicationByIdOnce(schedule.medicationId)
                ?: return@forEach

            if (medication.estado.key != "ACTIVO") return@forEach

            // Schedule alarms for next 7 days
            val today = System.currentTimeMillis()
            for (dayOffset in 0..6) {
                val targetDay = today + dayOffset * 24 * 60 * 60 * 1000L
                val times = DateUtils.getTimesForScheduleOnDay(schedule, targetDay)
                times.forEach { timeMillis ->
                    if (timeMillis > System.currentTimeMillis()) {
                        scheduleExactAlarm(alarmManager, schedule, medication.id, timeMillis)
                    }
                }
            }
        }

        return Result.success()
    }

    private fun scheduleExactAlarm(
        alarmManager: AlarmManager,
        schedule: Schedule,
        medicationId: String,
        triggerAtMillis: Long
    ) {
        val requestCode = "${schedule.id}_$triggerAtMillis".hashCode()
        val intent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = AppConstants.ACTION_MEDICATION_ALARM
            putExtra(AppConstants.EXTRA_MEDICATION_ID, medicationId)
            putExtra("schedule_id", schedule.id)
            putExtra("trigger_time", triggerAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // USE_EXACT_ALARM not granted; fall back to inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
