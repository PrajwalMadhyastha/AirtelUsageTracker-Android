package com.prajwal.utilities.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prajwal.utilities.core.navigation.AppNavHost
import com.prajwal.utilities.core.theme.UtilitiesTheme
import com.prajwal.utilities.tools.wealthtracker.worker.PortfolioSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val syncRequest = PeriodicWorkRequestBuilder<PortfolioSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "WealthTrackerSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        setContent {
            UtilitiesTheme {
                AppNavHost()
            }
        }
    }
}
