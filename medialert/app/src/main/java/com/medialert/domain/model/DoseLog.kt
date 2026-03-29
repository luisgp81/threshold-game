package com.medialert.domain.model

data class DoseLog(
    val id: String,
    val medicationId: String,
    val scheduleId: String,
    val fechaProgramada: Long,
    val fechaTomada: Long?,
    val estado: DoseEstado,
    val fotoPath: String?,
    val notas: String?
)

enum class DoseEstado(val key: String) {
    TOMADA("TOMADA"),
    OMITIDA("OMITIDA"),
    PENDIENTE("PENDIENTE"),
    TOMADA_TARDE("TOMADA_TARDE");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: PENDIENTE
    }
}
