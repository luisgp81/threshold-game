package com.medialert.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId"), Index("scheduleId"), Index("fechaProgramada")]
)
data class DoseLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicationId: String,
    val scheduleId: String,
    val fechaProgramada: Long,
    val fechaTomada: Long?,
    val estado: String, // TOMADA, OMITIDA, PENDIENTE, TOMADA_TARDE
    val fotoPath: String?,
    val notas: String?
)
