package com.medialert.di

import com.medialert.data.repository.DoseLogRepositoryImpl
import com.medialert.data.repository.MedicationRepositoryImpl
import com.medialert.data.repository.ScheduleRepositoryImpl
import com.medialert.domain.repository.DoseLogRepository
import com.medialert.domain.repository.MedicationRepository
import com.medialert.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMedicationRepository(impl: MedicationRepositoryImpl): MedicationRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindDoseLogRepository(impl: DoseLogRepositoryImpl): DoseLogRepository
}
