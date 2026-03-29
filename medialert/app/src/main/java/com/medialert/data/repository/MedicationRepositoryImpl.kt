package com.medialert.data.repository

import com.medialert.data.db.dao.MedicationDao
import com.medialert.data.db.entities.MedicationEntity
import com.medialert.domain.model.FormaFarmaceutica
import com.medialert.domain.model.Medication
import com.medialert.domain.model.MedicationEstado
import com.medialert.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicationRepositoryImpl @Inject constructor(
    private val dao: MedicationDao
) : MedicationRepository {

    override fun getAllMedications(): Flow<List<Medication>> =
        dao.getAllMedications().map { list -> list.map { it.toDomain() } }

    override fun getMedicationsByStatus(estado: MedicationEstado): Flow<List<Medication>> =
        dao.getMedicationsByStatus(estado.key).map { list -> list.map { it.toDomain() } }

    override fun getMedicationById(id: String): Flow<Medication?> =
        dao.getMedicationById(id).map { it?.toDomain() }

    override suspend fun getMedicationByIdOnce(id: String): Medication? =
        dao.getMedicationByIdOnce(id)?.toDomain()

    override fun getLowStockMedications(): Flow<List<Medication>> =
        dao.getLowStockMedications().map { list -> list.map { it.toDomain() } }

    override suspend fun saveMedication(medication: Medication) =
        dao.insertMedication(medication.toEntity())

    override suspend fun updateStock(id: String, stock: Int) =
        dao.updateStock(id, stock)

    override suspend fun updateStatus(id: String, estado: MedicationEstado) =
        dao.updateStatus(id, estado.key)

    override suspend fun deleteMedication(id: String) =
        dao.deleteMedicationById(id)
}

fun MedicationEntity.toDomain() = Medication(
    id = id,
    nombre = nombre,
    principioActivo = principioActivo,
    formaFarmaceutica = FormaFarmaceutica.fromKey(formaFarmaceutica),
    fotoPath = fotoPath,
    dosisAmount = dosisAmount,
    dosisUnit = dosisUnit,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    urlProspecto = urlProspecto,
    instrucciones = instrucciones,
    estado = MedicationEstado.fromKey(estado),
    registroFotoActivo = registroFotoActivo,
    fechaCreacion = fechaCreacion
)

fun Medication.toEntity() = MedicationEntity(
    id = id,
    nombre = nombre,
    principioActivo = principioActivo,
    formaFarmaceutica = formaFarmaceutica.key,
    fotoPath = fotoPath,
    dosisAmount = dosisAmount,
    dosisUnit = dosisUnit,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    urlProspecto = urlProspecto,
    instrucciones = instrucciones,
    estado = estado.key,
    registroFotoActivo = registroFotoActivo,
    fechaCreacion = fechaCreacion
)
