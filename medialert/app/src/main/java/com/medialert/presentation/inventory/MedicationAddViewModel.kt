package com.medialert.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medialert.domain.model.Medication
import com.medialert.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationAddViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    fun saveMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.saveMedication(medication)
        }
    }
}
