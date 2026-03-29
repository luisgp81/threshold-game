package com.medialert.data.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.medialert.data.db.entities.DoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Query("""
        SELECT * FROM dose_logs
        WHERE fechaProgramada >= :startOfDay AND fechaProgramada < :endOfDay
        ORDER BY fechaProgramada ASC
    """)
    fun getDoseLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DoseLogEntity>>

    @Query("""
        SELECT * FROM dose_logs
        WHERE medicationId = :medicationId
        ORDER BY fechaProgramada DESC
    """)
    fun getDoseLogsForMedication(medicationId: String): PagingSource<Int, DoseLogEntity>

    @Query("""
        SELECT * FROM dose_logs
        WHERE fechaProgramada >= :from AND fechaProgramada <= :to
        ORDER BY fechaProgramada DESC
    """)
    fun getDoseLogsBetween(from: Long, to: Long): PagingSource<Int, DoseLogEntity>

    @Query("""
        SELECT * FROM dose_logs
        WHERE medicationId = :medicationId
          AND fechaProgramada >= :from
          AND fechaProgramada <= :to
        ORDER BY fechaProgramada DESC
    """)
    suspend fun getDoseLogsForMedicationBetween(
        medicationId: String,
        from: Long,
        to: Long
    ): List<DoseLogEntity>

    @Query("""
        SELECT * FROM dose_logs
        WHERE estado = 'PENDIENTE' AND fechaProgramada < :now
        ORDER BY fechaProgramada ASC
    """)
    suspend fun getOverduePendingDoses(now: Long): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getDoseLogById(id: String): DoseLogEntity?

    @Query("""
        SELECT COUNT(*) FROM dose_logs
        WHERE medicationId = :medicationId
          AND estado IN ('TOMADA', 'TOMADA_TARDE')
          AND fechaProgramada >= :from
          AND fechaProgramada <= :to
    """)
    suspend fun getTakenCountForMedication(medicationId: String, from: Long, to: Long): Int

    @Query("""
        SELECT COUNT(*) FROM dose_logs
        WHERE medicationId = :medicationId
          AND fechaProgramada >= :from
          AND fechaProgramada <= :to
    """)
    suspend fun getTotalCountForMedication(medicationId: String, from: Long, to: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(doseLog: DoseLogEntity)

    @Update
    suspend fun updateDoseLog(doseLog: DoseLogEntity)

    @Query("UPDATE dose_logs SET estado = :estado, fechaTomada = :timestamp WHERE id = :id")
    suspend fun updateDoseStatus(id: String, estado: String, timestamp: Long?)

    @Query("UPDATE dose_logs SET fotoPath = :fotoPath WHERE id = :id")
    suspend fun updateDosePhoto(id: String, fotoPath: String)

    @Delete
    suspend fun deleteDoseLog(doseLog: DoseLogEntity)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId")
    suspend fun deleteDoseLogsForMedication(medicationId: String)
}
