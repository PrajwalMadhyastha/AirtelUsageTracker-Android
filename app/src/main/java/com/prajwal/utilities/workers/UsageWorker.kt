package com.prajwal.utilities.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prajwal.utilities.data.UsageRepository

class UsageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "UsageWorker"
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "UsageWorker started")
        
        val repository = UsageRepository(applicationContext)
        
        return try {
            if (!repository.checkIsOnboardingCompleted()) {
                Log.d(TAG, "Skipping background work: Onboarding not completed")
                return Result.success()
            }

            val result = repository.fetchAndUpdateUsage()
            
            if (result.isSuccess) {
                val usageData = result.getOrNull()
                Log.d(TAG, "Usage updated successfully: ${usageData?.toGigabytes()} GB")
                Result.success()
            } else {
                Log.e(TAG, "Failed to update usage", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in UsageWorker", e)
            Result.retry()
        }
    }
}
