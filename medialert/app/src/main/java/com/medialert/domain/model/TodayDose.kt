package com.medialert.domain.model

/**
 * Composite model combining a DoseLog entry with its medication data,
 * used to display today's schedule.
 */
data class TodayDose(
    val doseLog: DoseLog,
    val medication: Medication,
    val schedule: Schedule
)
