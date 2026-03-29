package com.medialert.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.medialert.data.db.dao.DoseLogDao
import com.medialert.data.db.entities.DoseLogEntity
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.DoseLog
import com.medialert.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DoseLogRepositoryImpl @Inject constructor(
    private val dao: DoseLogDao
) : DoseLogRepository {

    override fun getDoseLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DoseLog>> =
        dao.getDoseLogsForDay(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }

    override fun getDoseLogsForMedication(medicationId: String): Flow<PagingData<DoseLog>> =
        Pager(PagingConfig(pageSize = 30)) {
            dao.getDoseLogsForMedication(medicationId)
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun getDoseLogsBetween(from: Long, to: Long): Flow<PagingData<DoseLog>> =
        Pager(PagingConfig(pageSize = 30)) {
            dao.getDoseLogsBetween(from, to)
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override suspend fun getDoseLogsForMedicationBetween(
        medicationId: String,
        from: Long,
        to: Long
    ): List<DoseLog> =
        dao.getDoseLogsForMedicationBetween(medicationId, from, to).map { it.toDomain() }

    override suspend fun getOverduePendingDoses(now: Long): List<DoseLog> =
        dao.getOverduePendingDoses(now).map { it.toDomain() }

    override suspend fun getDoseLogById(id: String): DoseLog? =
        dao.getDoseLogById(id)?.toDomain()

    override suspend fun getAdherenceForMedication(
        medicationId: String,
        from: Long,
        to: Long
    ): Float {
        val total = dao.getTotalCountForMedication(medicationId, from, to)
        if (total == 0) return 1f
        val taken = dao.getTakenCountForMedication(medicationId, from, to)
        return taken.toFloat() / total.toFloat()
    }

    override suspend fun saveDoseLog(doseLog: DoseLog) =
        dao.insertDoseLog(doseLog.toEntity())

    override suspend fun updateDoseStatus(id: String, estado: DoseEstado, timestamp: Long?) =
        dao.updateDoseStatus(id, estado.key, timestamp)

    override suspend fun updateDosePhoto(id: String, fotoPath: String) =
        dao.updateDosePhoto(id, fotoPath)

    override suspend fun deleteDoseLogsForMedication(medicationId: String) =
        dao.deleteDoseLogsForMedication(medicationId)
}

fun DoseLogEntity.toDomain() = DoseLog(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    fechaProgramada = fechaProgramada,
    fechaTomada = fechaTomada,
    estado = DoseEstado.fromKey(estado),
    fotoPath = fotoPath,
    notas = notas
)

fun DoseLog.toEntity() = DoseLogEntity(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    fechaProgramada = fechaProgramada,
    fechaTomada = fechaTomada,
    estado = estado.key,
    fotoPath = fotoPath,
    notas = notas
)
