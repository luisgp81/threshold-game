package com.medialert.domain.usecase

import com.medialert.domain.model.TodayDose
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.MedicationRepository
import com.medialert.domain.repository.ScheduleRepository
import com.medialert.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTodayDosesUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val medicationRepository: MedicationRepository,
    private val scheduleRepository: ScheduleRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: Long = System.currentTimeMillis()): Flow<List<TodayDose>> {
        val (start, end) = DateUtils.getDayBounds(date)
        return doseLogRepository.getDoseLogsForDay(start, end).flatMapLatest { doseLogs ->
            flow {
                val todayDoses = doseLogs.mapNotNull { doseLog ->
                    val medication = medicationRepository.getMedicationByIdOnce(doseLog.medicationId)
                        ?: return@mapNotNull null
                    val schedule = scheduleRepository.getScheduleById(doseLog.scheduleId)
                        ?: return@mapNotNull null
                    TodayDose(doseLog, medication, schedule)
                }.sortedBy { it.doseLog.fechaProgramada }
                emit(todayDoses)
            }
        }
    }
}
