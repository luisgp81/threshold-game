package com.medialert.domain.usecase

import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.DoseLog
import com.medialert.domain.model.MedicationEstado
import com.medialert.domain.model.ScheduleTipo
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.MedicationRepository
import com.medialert.domain.repository.ScheduleRepository
import com.medialert.utils.DateUtils
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class GenerateDosesForTodayUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: ScheduleRepository,
    private val doseLogRepository: DoseLogRepository
) {
    suspend operator fun invoke(date: Long = System.currentTimeMillis()) {
        val (startOfDay, endOfDay) = DateUtils.getDayBounds(date)
        val existingDoses = doseLogRepository.getDoseLogsForDay(startOfDay, endOfDay)

        val activeMedications = medicationRepository.getMedicationsByStatus(MedicationEstado.ACTIVO)

        // Collect existing dose keys to avoid duplicates
        val existingKeys = mutableSetOf<String>() // "scheduleId_HH:mm"

        activeMedications.collect { medications ->
            medications.forEach { medication ->
                val schedules = scheduleRepository.getActiveSchedulesForMedication(medication.id)
                schedules.forEach { schedule ->
                    if (schedule.fechaFin != null && schedule.fechaFin < startOfDay) return@forEach
                    if (schedule.fechaInicio > endOfDay) return@forEach

                    val timesToday = DateUtils.getTimesForScheduleOnDay(schedule, date)
                    timesToday.forEach { timeMillis ->
                        val key = "${schedule.id}_$timeMillis"
                        if (!existingKeys.contains(key)) {
                            existingKeys.add(key)
                            val doseLog = DoseLog(
                                id = UUID.randomUUID().toString(),
                                medicationId = medication.id,
                                scheduleId = schedule.id,
                                fechaProgramada = timeMillis,
                                fechaTomada = null,
                                estado = DoseEstado.PENDIENTE,
                                fotoPath = null,
                                notas = null
                            )
                            doseLogRepository.saveDoseLog(doseLog)
                        }
                    }
                }
            }
        }
    }
}
