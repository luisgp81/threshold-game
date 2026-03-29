package com.medialert.domain.repository

import androidx.paging.PagingData
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.DoseLog
import kotlinx.coroutines.flow.Flow

interface DoseLogRepository {
    fun getDoseLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DoseLog>>
    fun getDoseLogsForMedication(medicationId: String): Flow<PagingData<DoseLog>>
    fun getDoseLogsBetween(from: Long, to: Long): Flow<PagingData<DoseLog>>
    suspend fun getDoseLogsForMedicationBetween(medicationId: String, from: Long, to: Long): List<DoseLog>
    suspend fun getOverduePendingDoses(now: Long): List<DoseLog>
    suspend fun getDoseLogById(id: String): DoseLog?
    suspend fun getAdherenceForMedication(medicationId: String, from: Long, to: Long): Float
    suspend fun saveDoseLog(doseLog: DoseLog)
    suspend fun updateDoseStatus(id: String, estado: DoseEstado, timestamp: Long?)
    suspend fun updateDosePhoto(id: String, fotoPath: String)
    suspend fun deleteDoseLogsForMedication(medicationId: String)
}
