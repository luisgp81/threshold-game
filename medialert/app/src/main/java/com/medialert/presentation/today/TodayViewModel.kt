package com.medialert.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.TodayDose
import com.medialert.domain.usecase.GetTodayDosesUseCase
import com.medialert.domain.usecase.MarkDoseTakenUseCase
import com.medialert.domain.repository.DoseLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val doses: List<TodayDose> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val totalDoses: Int get() = doses.size
    val completedDoses: Int get() = doses.count {
        it.doseLog.estado == DoseEstado.TOMADA || it.doseLog.estado == DoseEstado.TOMADA_TARDE
    }
    val progressFraction: Float get() = if (totalDoses == 0) 1f else completedDoses.toFloat() / totalDoses
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val getTodayDoses: GetTodayDosesUseCase,
    private val markDoseTaken: MarkDoseTakenUseCase,
    private val doseLogRepository: DoseLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        loadTodayDoses()
    }

    private fun loadTodayDoses() {
        viewModelScope.launch {
            getTodayDoses()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { doses ->
                    _uiState.update { it.copy(doses = doses, isLoading = false, error = null) }
                }
        }
    }

    fun markTaken(doseLogId: String, photoPath: String? = null) {
        viewModelScope.launch {
            markDoseTaken(doseLogId, photoPath)
        }
    }

    fun markSkipped(doseLogId: String) {
        viewModelScope.launch {
            doseLogRepository.updateDoseStatus(doseLogId, DoseEstado.OMITIDA, null)
        }
    }

    fun refresh() {
        loadTodayDoses()
    }
}
