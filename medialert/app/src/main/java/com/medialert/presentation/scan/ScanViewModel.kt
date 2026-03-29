package com.medialert.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medialert.data.remote.*
import com.medialert.domain.model.*
import com.medialert.domain.repository.MedicationRepository
import com.medialert.domain.repository.ScheduleRepository
import com.medialert.presentation.navigation.ScanMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Capturing : ScanUiState()
    object Analyzing : ScanUiState()
    data class PrescriptionResult(val result: GeminiPrescriptionResponse, val imagePath: String) : ScanUiState()
    data class BoxResult(val result: GeminiBoxResponse, val imagePath: String) : ScanUiState()
    data class DoseResult(val result: GeminiDoseResponse, val imagePath: String) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
    object Saved : ScanUiState()
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun analyzeImage(imagePath: String, mode: ScanMode, medicationName: String = "", dose: String = "") {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Analyzing
            when (mode) {
                ScanMode.PRESCRIPTION -> {
                    when (val result = geminiApiService.analizarInforme(imagePath)) {
                        is GeminiResult.Success -> _uiState.value = ScanUiState.PrescriptionResult(result.data, imagePath)
                        is GeminiResult.Error -> _uiState.value = ScanUiState.Error(result.message)
                    }
                }
                ScanMode.BOX -> {
                    when (val result = geminiApiService.verificarCaja(imagePath)) {
                        is GeminiResult.Success -> _uiState.value = ScanUiState.BoxResult(result.data, imagePath)
                        is GeminiResult.Error -> _uiState.value = ScanUiState.Error(result.message)
                    }
                }
                ScanMode.DOSE -> {
                    when (val result = geminiApiService.registrarToma(imagePath, medicationName, dose)) {
                        is GeminiResult.Success -> _uiState.value = ScanUiState.DoseResult(result.data, imagePath)
                        is GeminiResult.Error -> _uiState.value = ScanUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun saveMedicationsFromPrescription(medications: List<GeminiMedication>) {
        viewModelScope.launch {
            medications.forEach { gm ->
                val medication = Medication(
                    id = UUID.randomUUID().toString(),
                    nombre = gm.nombre,
                    principioActivo = gm.principioActivo,
                    formaFarmaceutica = FormaFarmaceutica.fromKey(gm.formaFarmaceutica.lowercase()),
                    fotoPath = null,
                    dosisAmount = gm.dosis.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 1.0,
                    dosisUnit = gm.dosis.filter { it.isLetter() }.take(10),
                    stockActual = 0,
                    stockMinimo = 5,
                    urlProspecto = null,
                    instrucciones = gm.instruccionesEspeciales,
                    estado = MedicationEstado.ACTIVO,
                    registroFotoActivo = false,
                    fechaCreacion = System.currentTimeMillis()
                )
                medicationRepository.saveMedication(medication)

                if (gm.horariosSugeridos.isNotEmpty()) {
                    val schedule = Schedule(
                        id = UUID.randomUUID().toString(),
                        medicationId = medication.id,
                        tipo = ScheduleTipo.N_VECES_DIA,
                        intervalHoras = null,
                        horarios = gm.horariosSugeridos,
                        diasSemana = null,
                        fechaInicio = System.currentTimeMillis(),
                        fechaFin = gm.duracionDias?.let {
                            System.currentTimeMillis() + it * 24 * 60 * 60 * 1000L
                        },
                        activo = true
                    )
                    scheduleRepository.saveSchedule(schedule)
                }
            }
            _uiState.value = ScanUiState.Saved
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
