package com.medialert.data.db.dao

import androidx.room.*
import com.medialert.data.db.entities.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {

    @Query("SELECT * FROM prescriptions ORDER BY fechaEscaneo DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: String): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Query("UPDATE prescriptions SET procesado = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: String)

    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)
}
