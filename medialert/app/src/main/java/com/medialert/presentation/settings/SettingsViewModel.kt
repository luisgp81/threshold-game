package com.medialert.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medialert.data.repository.UserPreferencesRepository
import com.medialert.utils.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userMode: String = AppConstants.MODE_CUIDADOR,
    val theme: String = AppConstants.THEME_SYSTEM,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val pin: String? = null,
    val googleEmail: String = "",
    val lastBackupTimestamp: Long? = null,
    val isPinCorrect: Boolean? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefsRepository.userMode,
                prefsRepository.theme,
                prefsRepository.emergencyContactName,
                prefsRepository.emergencyContactPhone,
                prefsRepository.pin
            ) { mode, theme, contactName, contactPhone, pin ->
                _uiState.update {
                    it.copy(
                        userMode = mode,
                        theme = theme,
                        emergencyContactName = contactName ?: "",
                        emergencyContactPhone = contactPhone ?: "",
                        pin = pin
                    )
                }
            }.collect()
        }
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedPin = _uiState.value.pin ?: return true // No PIN set — allow access
        return inputPin == storedPin
    }

    fun setPin(newPin: String) {
        viewModelScope.launch { prefsRepository.setPin(newPin) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { prefsRepository.setTheme(theme) }
    }

    fun setUserMode(mode: String) {
        viewModelScope.launch { prefsRepository.setUserMode(mode) }
    }

    fun setEmergencyContact(name: String, phone: String) {
        viewModelScope.launch { prefsRepository.setEmergencyContact(name, phone) }
    }

    fun logout() {
        viewModelScope.launch { prefsRepository.clearUserData() }
    }
}
