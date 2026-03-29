package com.medialert.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.medialert.data.db.dao.*
import com.medialert.data.db.entities.*

@Database(
    entities = [
        MedicationEntity::class,
        ScheduleEntity::class,
        DoseLogEntity::class,
        PrescriptionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MediAlertDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun prescriptionDao(): PrescriptionDao

    companion object {
        const val DATABASE_NAME = "medialert.db"
    }
}
