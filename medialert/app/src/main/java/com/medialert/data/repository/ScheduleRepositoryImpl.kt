package com.medialert.data.repository

import com.medialert.data.db.dao.ScheduleDao
import com.medialert.data.db.entities.ScheduleEntity
import com.medialert.domain.model.Schedule
import com.medialert.domain.model.ScheduleTipo
import com.medialert.domain.repository.ScheduleRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val dao: ScheduleDao,
    private val moshi: Moshi
) : ScheduleRepository {

    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val intListAdapter = moshi.adapter<List<Int>>(
        Types.newParameterizedType(List::class.java, Integer::class.java)
    )

    override fun getSchedulesForMedication(medicationId: String): Flow<List<Schedule>> =
        dao.getSchedulesForMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveSchedulesForMedication(medicationId: String): List<Schedule> =
        dao.getActiveSchedulesForMedication(medicationId).map { it.toDomain() }

    override suspend fun getAllActiveSchedules(): List<Schedule> =
        dao.getAllActiveSchedules().map { it.toDomain() }

    override suspend fun getScheduleById(id: String): Schedule? =
        dao.getScheduleById(id)?.toDomain()

    override suspend fun saveSchedule(schedule: Schedule) =
        dao.insertSchedule(schedule.toEntity())

    override suspend fun setScheduleActive(id: String, activo: Boolean) =
        dao.setScheduleActive(id, activo)

    override suspend fun deleteSchedule(id: String) {
        dao.getScheduleById(id)?.let { dao.deleteSchedule(it) }
    }

    override suspend fun deleteSchedulesForMedication(medicationId: String) =
        dao.deleteSchedulesForMedication(medicationId)

    private fun ScheduleEntity.toDomain() = Schedule(
        id = id,
        medicationId = medicationId,
        tipo = ScheduleTipo.fromKey(tipo),
        intervalHoras = intervalHoras,
        horarios = stringListAdapter.fromJson(horariosJson) ?: emptyList(),
        diasSemana = diasSemanaJson?.let { intListAdapter.fromJson(it) },
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        activo = activo
    )

    private fun Schedule.toEntity() = ScheduleEntity(
        id = id,
        medicationId = medicationId,
        tipo = tipo.key,
        intervalHoras = intervalHoras,
        horariosJson = stringListAdapter.toJson(horarios),
        diasSemanaJson = diasSemana?.let { intListAdapter.toJson(it) },
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        activo = activo
    )
}
