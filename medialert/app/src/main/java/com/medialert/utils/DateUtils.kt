package com.medialert.utils

import com.medialert.domain.model.Schedule
import com.medialert.domain.model.ScheduleTipo
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun getDayBounds(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    fun getTimesForScheduleOnDay(schedule: Schedule, date: Long): List<Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat

        return when (schedule.tipo) {
            ScheduleTipo.N_VECES_DIA, ScheduleTipo.CADA_N_HORAS -> {
                schedule.horarios.mapNotNull { timeStr ->
                    parseTimeToMillis(timeStr, date)
                }
            }
            ScheduleTipo.DIAS_ESPECIFICOS -> {
                val diasSemana = schedule.diasSemana ?: return emptyList()
                if (dayOfWeek !in diasSemana) return emptyList()
                schedule.horarios.mapNotNull { timeStr ->
                    parseTimeToMillis(timeStr, date)
                }
            }
        }
    }

    /** Parses "HH:mm" into an absolute timestamp on the given day */
    fun parseTimeToMillis(timeStr: String, date: Long): Long? {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDayOfWeek(timestamp: Long): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            .replaceFirstChar { it.uppercase() }
    }

    /** Calculates how many days a stock will last given doses per day */
    fun daysOfStockRemaining(stock: Int, dosesPerDay: Float): Int {
        if (dosesPerDay <= 0f) return Int.MAX_VALUE
        return (stock / dosesPerDay).toInt()
    }

    fun stockEndDate(stock: Int, dosesPerDay: Float): Long {
        val days = daysOfStockRemaining(stock, dosesPerDay)
        return System.currentTimeMillis() + days * 24 * 60 * 60 * 1000L
    }

    fun startOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
