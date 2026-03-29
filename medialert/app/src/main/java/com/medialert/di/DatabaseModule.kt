package com.medialert.di

import android.content.Context
import androidx.room.Room
import com.medialert.data.db.MediAlertDatabase
import com.medialert.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediAlertDatabase =
        Room.databaseBuilder(context, MediAlertDatabase::class.java, MediAlertDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMedicationDao(db: MediAlertDatabase): MedicationDao = db.medicationDao()

    @Provides
    fun provideScheduleDao(db: MediAlertDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideDoseLogDao(db: MediAlertDatabase): DoseLogDao = db.doseLogDao()

    @Provides
    fun providePrescriptionDao(db: MediAlertDatabase): PrescriptionDao = db.prescriptionDao()
}
