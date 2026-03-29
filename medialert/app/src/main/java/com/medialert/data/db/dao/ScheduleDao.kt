package com.medialert.data.db.dao

import androidx.room.*
import com.medialert.data.db.entities.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    fun getSchedulesForMedication(medicationId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId AND activo = 1")
    suspend fun getActiveSchedulesForMedication(medicationId: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE activo = 1")
    suspend fun getAllActiveSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET activo = :activo WHERE id = :id")
    suspend fun setScheduleActive(id: String, activo: Boolean)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: String)
}
