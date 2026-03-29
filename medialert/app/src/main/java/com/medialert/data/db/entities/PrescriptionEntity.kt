package com.medialert.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fotoPath: String,
    val textoOCR: String?,
    val respuestaGemini: String,
    val procesado: Boolean = false,
    val fechaEscaneo: Long = System.currentTimeMillis()
)
