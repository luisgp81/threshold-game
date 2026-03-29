package com.medialert.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class ScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicationId: String,
    val tipo: String, // CADA_N_HORAS, N_VECES_DIA, DIAS_ESPECIFICOS
    val intervalHoras: Int?,
    val horariosJson: String, // JSON array de "HH:mm"
    val diasSemanaJson: String?, // JSON array de ints 1-7
    val fechaInicio: Long,
    val fechaFin: Long?,
    val activo: Boolean = true
)
