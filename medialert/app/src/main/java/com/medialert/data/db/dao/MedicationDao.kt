package com.medialert.data.db.dao

import androidx.room.*
import com.medialert.data.db.entities.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications ORDER BY fechaCreacion DESC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE estado = :estado ORDER BY fechaCreacion DESC")
    fun getMedicationsByStatus(estado: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationById(id: String): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationByIdOnce(id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE estado = 'ACTIVO' AND stockActual <= stockMinimo")
    fun getLowStockMedications(): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET stockActual = :stock WHERE id = :id")
    suspend fun updateStock(id: String, stock: Int)

    @Query("UPDATE medications SET estado = :estado WHERE id = :id")
    suspend fun updateStatus(id: String, estado: String)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: String)
}
