package com.medialert.domain.repository

import com.medialert.domain.model.Medication
import com.medialert.domain.model.MedicationEstado
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    fun getAllMedications(): Flow<List<Medication>>
    fun getMedicationsByStatus(estado: MedicationEstado): Flow<List<Medication>>
    fun getMedicationById(id: String): Flow<Medication?>
    suspend fun getMedicationByIdOnce(id: String): Medication?
    fun getLowStockMedications(): Flow<List<Medication>>
    suspend fun saveMedication(medication: Medication)
    suspend fun updateStock(id: String, stock: Int)
    suspend fun updateStatus(id: String, estado: MedicationEstado)
    suspend fun deleteMedication(id: String)
}
