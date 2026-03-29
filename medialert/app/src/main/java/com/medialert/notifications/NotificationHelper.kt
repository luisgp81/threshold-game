package com.medialert.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medialert.MainActivity
import com.medialert.R
import com.medialert.domain.model.DoseLog
import com.medialert.domain.model.Medication
import com.medialert.receivers.NotificationActionReceiver
import com.medialert.utils.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val reminderChannel = NotificationChannel(
            AppConstants.CHANNEL_MEDICATION_REMINDER,
            context.getString(R.string.channel_reminder_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_reminder_description)
            enableVibration(true)
            enableLights(true)
        }

        val stockChannel = NotificationChannel(
            AppConstants.CHANNEL_STOCK_ALERT,
            context.getString(R.string.channel_stock_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_stock_description)
        }

        val backupChannel = NotificationChannel(
            AppConstants.CHANNEL_BACKUP,
            context.getString(R.string.channel_backup_name),
            NotificationManager.IMPORTANCE_LOW
        )

        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemManager.createNotificationChannels(listOf(reminderChannel, stockChannel, backupChannel))
    }

    fun buildMedicationReminderNotification(
        doseLog: DoseLog,
        medication: Medication,
        notificationId: Int
    ): Notification {
        val takenIntent = buildActionIntent(
            action = AppConstants.ACTION_DOSE_TAKEN,
            doseLogId = doseLog.id,
            notificationId = notificationId
        )
        val snoozeIntent = buildActionIntent(
            action = AppConstants.ACTION_DOSE_SNOOZE,
            doseLogId = doseLog.id,
            notificationId = notificationId
        )
        val skipIntent = buildActionIntent(
            action = AppConstants.ACTION_DOSE_SKIPPED,
            doseLogId = doseLog.id,
            notificationId = notificationId
        )

        val contentIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppConstants.EXTRA_MEDICATION_ID, medication.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, AppConstants.CHANNEL_MEDICATION_REMINDER)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(context.getString(R.string.notif_title_reminder, medication.nombre))
            .setContentText(buildDoseText(medication))
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(R.drawable.ic_check, context.getString(R.string.notif_action_taken), takenIntent)
            .addAction(R.drawable.ic_snooze, context.getString(R.string.notif_action_snooze), snoozeIntent)
            .addAction(R.drawable.ic_skip, context.getString(R.string.notif_action_skip), skipIntent)

        // Use BigPictureStyle if medication photo exists
        medication.fotoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as android.graphics.Bitmap?)
                    )
                }
            }
        } ?: run {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(buildDoseText(medication))
            )
        }

        return builder.build()
    }

    fun buildFollowUpNotification(
        doseLog: DoseLog,
        medication: Medication,
        notificationId: Int,
        isFinal: Boolean = false
    ): Notification {
        val takenIntent = buildActionIntent(
            action = AppConstants.ACTION_DOSE_TAKEN,
            doseLogId = doseLog.id,
            notificationId = notificationId
        )

        val title = if (isFinal) {
            context.getString(R.string.notif_title_final_reminder, medication.nombre)
        } else {
            context.getString(R.string.notif_title_followup, medication.nombre)
        }

        return NotificationCompat.Builder(context, AppConstants.CHANNEL_MEDICATION_REMINDER)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_followup_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_check, context.getString(R.string.notif_action_taken_yes), takenIntent)
            .build()
    }

    fun buildLowStockNotification(medication: Medication): Notification {
        return NotificationCompat.Builder(context, AppConstants.CHANNEL_STOCK_ALERT)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(context.getString(R.string.notif_title_low_stock))
            .setContentText(context.getString(R.string.notif_low_stock_text, medication.nombre, medication.stockActual))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    fun showNotification(id: Int, notification: Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            // Permission not granted; ignore silently
        }
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }

    private fun buildActionIntent(action: String, doseLogId: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(AppConstants.EXTRA_DOSE_LOG_ID, doseLogId)
            putExtra(AppConstants.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildDoseText(medication: Medication): String {
        val doseStr = "${medication.dosisAmount} ${medication.dosisUnit}"
        return if (!medication.instrucciones.isNullOrBlank()) {
            "$doseStr — ${medication.instrucciones}"
        } else {
            doseStr
        }
    }
}
