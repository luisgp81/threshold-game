package com.medialert.utils

object AppConstants {
    // User modes
    const val MODE_CUIDADOR = "CUIDADOR"
    const val MODE_MAYOR = "MAYOR"

    // Themes
    const val THEME_LIGHT = "LIGHT"
    const val THEME_DARK = "DARK"
    const val THEME_SYSTEM = "SYSTEM"

    // Notification channels
    const val CHANNEL_MEDICATION_REMINDER = "medication_reminder"
    const val CHANNEL_STOCK_ALERT = "stock_alert"
    const val CHANNEL_BACKUP = "backup"

    // Notification IDs base (actual ID = base + hashCode of doseLogId)
    const val NOTIF_ID_REMINDER_BASE = 1000
    const val NOTIF_ID_FOLLOWUP_BASE = 2000
    const val NOTIF_ID_FINAL_BASE = 3000
    const val NOTIF_ID_LOW_STOCK = 9000

    // WorkManager tags
    const val WORK_TAG_DAILY_GENERATOR = "daily_dose_generator"
    const val WORK_TAG_BACKUP = "backup_work"
    const val WORK_TAG_MEDICATION_PREFIX = "medication_alarm_"

    // Intent extras
    const val EXTRA_DOSE_LOG_ID = "dose_log_id"
    const val EXTRA_MEDICATION_ID = "medication_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_ACTION_TYPE = "action_type"

    // Actions
    const val ACTION_DOSE_TAKEN = "com.medialert.action.DOSE_TAKEN"
    const val ACTION_DOSE_SNOOZE = "com.medialert.action.DOSE_SNOOZE"
    const val ACTION_DOSE_SKIPPED = "com.medialert.action.DOSE_SKIPPED"
    const val ACTION_MEDICATION_ALARM = "com.medialert.action.MEDICATION_ALARM"

    // Snooze duration
    const val SNOOZE_MINUTES = 5L

    // Follow-up delays (minutes)
    const val FOLLOWUP_FIRST_MINUTES = 5L
    const val FOLLOWUP_SECOND_MINUTES = 15L

    // Elder mode UI constants
    const val ELDER_MIN_FONT_SP = 20
    const val ELDER_MIN_BUTTON_HEIGHT_DP = 64

    // PIN settings
    const val PIN_LENGTH = 4

    // Low confidence threshold for Gemini results
    const val GEMINI_LOW_CONFIDENCE_THRESHOLD = 0.7
}
