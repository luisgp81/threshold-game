package com.medialert.domain.usecase

import com.medialert.domain.model.DoseEstado
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.MedicationRepository
import javax.inject.Inject

class MarkDoseTakenUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(doseLogId: String, photoPath: String? = null) {
        val now = System.currentTimeMillis()
        val doseLog = doseLogRepository.getDoseLogById(doseLogId) ?: return

        val scheduledTime = doseLog.fechaProgramada
        // Mark as TOMADA_TARDE if taken more than 30 minutes after scheduled time
        val estado = if (now - scheduledTime > 30 * 60 * 1000) DoseEstado.TOMADA_TARDE else DoseEstado.TOMADA

        doseLogRepository.updateDoseStatus(doseLogId, estado, now)

        // Update photo if captured
        if (photoPath != null) {
            doseLogRepository.updateDosePhoto(doseLogId, photoPath)
        }

        // Decrement stock
        val medication = medicationRepository.getMedicationByIdOnce(doseLog.medicationId) ?: return
        val newStock = maxOf(0, medication.stockActual - 1)
        medicationRepository.updateStock(medication.id, newStock)
    }
}
