package com.medialert.domain.model

data class Medication(
    val id: String,
    val nombre: String,
    val principioActivo: String?,
    val formaFarmaceutica: FormaFarmaceutica,
    val fotoPath: String?,
    val dosisAmount: Double,
    val dosisUnit: String,
    val stockActual: Int,
    val stockMinimo: Int,
    val urlProspecto: String?,
    val instrucciones: String?,
    val estado: MedicationEstado,
    val registroFotoActivo: Boolean,
    val fechaCreacion: Long
)

enum class FormaFarmaceutica(val key: String) {
    PASTILLA("pastilla"),
    CAPSULA("capsula"),
    JARABE("jarabe"),
    GOTAS("gotas"),
    INYECTABLE("inyectable"),
    PARCHE("parche"),
    OTRO("otro");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: OTRO
    }
}

enum class MedicationEstado(val key: String) {
    ACTIVO("ACTIVO"),
    PAUSADO("PAUSADO"),
    FINALIZADO("FINALIZADO");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: ACTIVO
    }
}
