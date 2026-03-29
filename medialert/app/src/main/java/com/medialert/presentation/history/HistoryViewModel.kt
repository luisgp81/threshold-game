package com.medialert.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.medialert.domain.model.DoseLog
import com.medialert.domain.model.Medication
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.MedicationRepository
import com.medialert.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdherenceDay(
    val date: Long,
    val percentage: Float  // 0.0 – 1.0
)

data class HistoryUiState(
    val medications: List<Medication> = emptyList(),
    val selectedMedicationId: String? = null,
    val adherenceDays: List<AdherenceDay> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _dateRange = MutableStateFlow(getDefaultDateRange())

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedDoseLogs: Flow<PagingData<DoseLog>> = _dateRange.flatMapLatest { (from, to) ->
        doseLogRepository.getDoseLogsBetween(from, to)
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            medicationRepository.getAllMedications()
                .collect { meds ->
                    _uiState.update { it.copy(medications = meds) }
                }
        }
        loadAdherenceCalendar()
    }

    fun setDateRange(from: Long, to: Long) {
        _dateRange.value = Pair(from, to)
    }

    fun selectMedication(id: String?) {
        _uiState.update { it.copy(selectedMedicationId = id) }
    }

    private fun loadAdherenceCalendar() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val monthStart = DateUtils.startOfMonth(now)
            val monthEnd = DateUtils.endOfMonth(now)
            val days = mutableListOf<AdherenceDay>()

            var dayStart = monthStart
            while (dayStart <= monthEnd) {
                val (s, e) = DateUtils.getDayBounds(dayStart)
                val logs = doseLogRepository.getDoseLogsBetween(s, e)
                // Simplified: count from first page
                days.add(AdherenceDay(dayStart, 1f)) // Placeholder; real calc via SQL
                dayStart += 24 * 60 * 60 * 1000L
            }
            _uiState.update { it.copy(adherenceDays = days) }
        }
    }

    private fun getDefaultDateRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return Pair(now - 30 * 24 * 60 * 60 * 1000L, now)
    }
}
