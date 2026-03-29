package com.medialert.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medialert.domain.model.Medication
import com.medialert.domain.model.MedicationEstado
import com.medialert.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val medications: List<Medication> = emptyList(),
    val isLoading: Boolean = true,
    val filter: MedicationEstado? = null,
    val error: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _filter = MutableStateFlow<MedicationEstado?>(null)
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _filter.flatMapLatest { filter ->
                if (filter == null) medicationRepository.getAllMedications()
                else medicationRepository.getMedicationsByStatus(filter)
            }
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .collect { meds ->
                _uiState.update { it.copy(medications = meds, isLoading = false, filter = _filter.value) }
            }
        }
    }

    fun setFilter(estado: MedicationEstado?) {
        _filter.value = estado
    }

    fun archiveMedication(id: String) {
        viewModelScope.launch {
            medicationRepository.updateStatus(id, MedicationEstado.FINALIZADO)
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            medicationRepository.deleteMedication(id)
        }
    }
}
