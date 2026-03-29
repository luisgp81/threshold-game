package com.medialert.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.medialert.utils.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_USER_MODE = stringPreferencesKey("user_mode")       // CUIDADOR, MAYOR
        val KEY_PIN = stringPreferencesKey("caretaker_pin")
        val KEY_EMERGENCY_CONTACT_NAME = stringPreferencesKey("emergency_contact_name")
        val KEY_EMERGENCY_CONTACT_PHONE = stringPreferencesKey("emergency_contact_phone")
        val KEY_LANGUAGE = stringPreferencesKey("language")         // es, en
        val KEY_THEME = stringPreferencesKey("theme")               // LIGHT, DARK, SYSTEM
        val KEY_GOOGLE_UID = stringPreferencesKey("google_uid")
        val KEY_GOOGLE_EMAIL = stringPreferencesKey("google_email")
        val KEY_LAST_BACKUP = longPreferencesKey("last_backup_timestamp")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val userMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_USER_MODE] ?: AppConstants.MODE_CUIDADOR }

    val pin: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_PIN] }

    val emergencyContactName: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_EMERGENCY_CONTACT_NAME] }

    val emergencyContactPhone: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_EMERGENCY_CONTACT_PHONE] }

    val theme: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_THEME] ?: AppConstants.THEME_SYSTEM }

    val googleUid: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_GOOGLE_UID] }

    val isOnboardingDone: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setUserMode(mode: String) = dataStore.edit { it[KEY_USER_MODE] = mode }
    suspend fun setPin(pin: String) = dataStore.edit { it[KEY_PIN] = pin }
    suspend fun setEmergencyContact(name: String, phone: String) = dataStore.edit {
        it[KEY_EMERGENCY_CONTACT_NAME] = name
        it[KEY_EMERGENCY_CONTACT_PHONE] = phone
    }
    suspend fun setTheme(theme: String) = dataStore.edit { it[KEY_THEME] = theme }
    suspend fun setGoogleUser(uid: String, email: String) = dataStore.edit {
        it[KEY_GOOGLE_UID] = uid
        it[KEY_GOOGLE_EMAIL] = email
    }
    suspend fun setOnboardingDone() = dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    suspend fun setLastBackup(timestamp: Long) = dataStore.edit { it[KEY_LAST_BACKUP] = timestamp }
    suspend fun clearUserData() = dataStore.edit { it.clear() }
}
