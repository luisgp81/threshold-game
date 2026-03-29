package com.medialert.presentation.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medialert.domain.model.Medication
import com.medialert.domain.model.MedicationEstado
import com.medialert.domain.model.Schedule
import com.medialert.domain.repository.MedicationRepository
import com.medialert.domain.repository.ScheduleRepository
import com.medialert.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationDetailUiState(
    val medication: Medication? = null,
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val daysRemaining: Int? = null
)

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val medicationId: String = checkNotNull(savedStateHandle["medicationId"])

    private val _uiState = MutableStateFlow(MedicationDetailUiState())
    val uiState: StateFlow<MedicationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                medicationRepository.getMedicationById(medicationId),
                scheduleRepository.getSchedulesForMedication(medicationId)
            ) { medication, schedules ->
                Pair(medication, schedules)
            }.collect { (medication, schedules) ->
                val dosesPerDay = schedules.filter { it.activo }
                    .sumOf { it.horarios.size }
                    .toFloat()
                val daysRemaining = medication?.let {
                    if (dosesPerDay > 0) DateUtils.daysOfStockRemaining(it.stockActual, dosesPerDay)
                    else null
                }
                _uiState.update {
                    it.copy(
                        medication = medication,
                        schedules = schedules,
                        isLoading = false,
                        daysRemaining = daysRemaining
                    )
                }
            }
        }
    }

    fun saveMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.saveMedication(medication)
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            scheduleRepository.saveSchedule(schedule)
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(id)
        }
    }

    fun updateStock(newStock: Int) {
        viewModelScope.launch {
            medicationRepository.updateStock(medicationId, newStock)
        }
    }

    fun archiveMedication() {
        viewModelScope.launch {
            medicationRepository.updateStatus(medicationId, MedicationEstado.FINALIZADO)
        }
    }
}
