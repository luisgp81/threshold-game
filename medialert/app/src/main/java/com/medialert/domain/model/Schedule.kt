package com.medialert.domain.model

data class Schedule(
    val id: String,
    val medicationId: String,
    val tipo: ScheduleTipo,
    val intervalHoras: Int?,
    val horarios: List<String>, // "HH:mm"
    val diasSemana: List<Int>?, // 1-7
    val fechaInicio: Long,
    val fechaFin: Long?,
    val activo: Boolean
)

enum class ScheduleTipo(val key: String) {
    CADA_N_HORAS("CADA_N_HORAS"),
    N_VECES_DIA("N_VECES_DIA"),
    DIAS_ESPECIFICOS("DIAS_ESPECIFICOS");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: N_VECES_DIA
    }
}
