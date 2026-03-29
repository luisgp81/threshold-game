package com.medialert.presentation.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val MODE_SELECT = "mode_select"

    // Main destinations
    const val TODAY = "today"
    const val INVENTORY = "inventory"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    // Detail / sub-screens
    const val MEDICATION_DETAIL = "medication_detail/{medicationId}"
    const val MEDICATION_ADD = "medication_add"
    const val SCAN = "scan/{scanMode}"
    const val ELDER_MODE = "elder_mode"
    const val PIN_LOCK = "pin_lock/{purpose}"
    const val PROSPECTUS_VIEWER = "prospectus/{url}"

    // Helpers
    fun medicationDetail(id: String) = "medication_detail/$id"
    fun scan(mode: ScanMode) = "scan/${mode.name}"
    fun pinLock(purpose: PinPurpose) = "pin_lock/${purpose.name}"
    fun prospectus(url: String) = "prospectus/${java.net.URLEncoder.encode(url, "UTF-8")}"
}

enum class ScanMode { PRESCRIPTION, BOX, DOSE }
enum class PinPurpose { UNLOCK_SETTINGS, SET_PIN, SWITCH_MODE }
