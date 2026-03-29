package com.medialert.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val principioActivo: String?,
    val formaFarmaceutica: String,
    val fotoPath: String?,
    val dosisAmount: Double,
    val dosisUnit: String,
    val stockActual: Int,
    val stockMinimo: Int,
    val urlProspecto: String?,
    val instrucciones: String?,
    val estado: String, // ACTIVO, PAUSADO, FINALIZADO
    val registroFotoActivo: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis()
)
