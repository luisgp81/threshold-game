package com.medialert.domain.repository

import com.medialert.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getSchedulesForMedication(medicationId: String): Flow<List<Schedule>>
    suspend fun getActiveSchedulesForMedication(medicationId: String): List<Schedule>
    suspend fun getAllActiveSchedules(): List<Schedule>
    suspend fun getScheduleById(id: String): Schedule?
    suspend fun saveSchedule(schedule: Schedule)
    suspend fun setScheduleActive(id: String, activo: Boolean)
    suspend fun deleteSchedule(id: String)
    suspend fun deleteSchedulesForMedication(medicationId: String)
}
