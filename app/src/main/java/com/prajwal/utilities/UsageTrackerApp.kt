package com.prajwal.utilities

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prajwal.utilities.tools.wifiusage.workers.UsageWorker
import java.util.concurrent.TimeUnit

class UsageTrackerApp : Application() {
    
    private val TAG = "UsageTrackerApp"
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application started, scheduling background worker")
        scheduleUsageWorker()
    }
    
    private fun scheduleUsageWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<UsageWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "usage_tracker_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        Log.d(TAG, "Background worker scheduled (15-minute intervals)")
    }
}
