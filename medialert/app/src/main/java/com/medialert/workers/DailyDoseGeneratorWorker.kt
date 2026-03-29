package com.medialert.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.medialert.domain.usecase.GenerateDosesForTodayUseCase
import com.medialert.utils.AppConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyDoseGeneratorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val generateDosesForToday: GenerateDosesForTodayUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            generateDosesForToday()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun buildPeriodicRequest(): PeriodicWorkRequest {
            // Calculate initial delay so it runs at midnight
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5) // 5 min past midnight
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val initialDelay = midnight.timeInMillis - now.timeInMillis

            return PeriodicWorkRequestBuilder<DailyDoseGeneratorWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(AppConstants.WORK_TAG_DAILY_GENERATOR)
                .setConstraints(Constraints.Builder().build())
                .build()
        }
    }
}
