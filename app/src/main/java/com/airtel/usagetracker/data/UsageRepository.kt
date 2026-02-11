package com.airtel.usagetracker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.airtel.usagetracker.BuildConfig
import com.airtel.usagetracker.data.models.RouterConfig
import com.airtel.usagetracker.data.models.ScrapedData
import com.airtel.usagetracker.data.models.ScrapingStatus
import com.airtel.usagetracker.data.models.UsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageRepository(private val context: Context) {
    
    private val TAG = "UsageRepository"
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("usage_data", Context.MODE_PRIVATE)
    
    private val userPreferences = UserPreferences(context)
    private val scraper = RouterScraper(context)
    
    // Database
    private val database = com.airtel.usagetracker.data.db.AppDatabase.getDatabase(context)
    private val usageDao = database.usageDao()
    
    private val _scrapingStatus = MutableStateFlow(ScrapingStatus.IDLE)
    val scrapingStatus: StateFlow<ScrapingStatus> = _scrapingStatus.asStateFlow()

    // Expose preferences
    val isOnboardingCompleted = userPreferences.isOnboardingCompleted
    val syncIntervalHours = userPreferences.syncIntervalHours
    val isAutoSyncEnabled = userPreferences.isAutoSyncEnabled

    suspend fun setOnboardingCompleted(completed: Boolean) {
        userPreferences.setOnboardingCompleted(completed)
        if (completed) {
            // Attempt migration when onboarding is completed (or just strictly on first run)
            migrateLegacyData()
        }
    }

    suspend fun setSyncInterval(hours: Int) {
        userPreferences.setSyncInterval(hours)
        scheduleBackgroundWork(hours)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        userPreferences.setAutoSyncEnabled(enabled)
        if (enabled) {
            // Re-schedule with current interval
            var currentInterval = 4
            try {
                userPreferences.syncIntervalHours.collect { 
                    currentInterval = it
                    throw Exception("ValueFound")
                }
            } catch (e: Exception) {
                if (e.message != "ValueFound") throw e
            }
            scheduleBackgroundWork(currentInterval)
        } else {
            cancelBackgroundWork()
        }
    }

    suspend fun checkIsOnboardingCompleted(): Boolean {
        var completed = false
        try {
            userPreferences.isOnboardingCompleted.collect {
                completed = it
                throw Exception("ValueFound")
            }
        } catch (e: Exception) {
            if (e.message != "ValueFound") throw e
        }
        return completed
    }

    private fun scheduleBackgroundWork(intervalHours: Int) {
        Log.d(TAG, "Scheduling background work every $intervalHours hours")
        val workManager = androidx.work.WorkManager.getInstance(context)
        
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED) // WiFi only
            .setRequiresBatteryNotLow(true)
            .build()
            
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.airtel.usagetracker.workers.UsageWorker>(
            intervalHours.toLong(), java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            "usage_tracker_work",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelBackgroundWork() {
        Log.d(TAG, "Cancelling background work")
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork("usage_tracker_work")
    }
    
    fun getUsageData(): UsageData {
        return UsageData(
            lastTx = prefs.getLong("last_tx", 0),
            lastRx = prefs.getLong("last_rx", 0),
            lastUptime = prefs.getLong("last_uptime", 0),
            totalBytesCum = prefs.getLong("total_bytes_cum", 0),
            lastUpdated = prefs.getString("last_updated", "") ?: ""
        )
    }
    
    // Expose history flow
    fun getUsageHistory() = usageDao.getAllUsage()
    
    // Migration: specific method to move SP data to DB if DB is empty
    suspend fun migrateLegacyData() {
        withContext(Dispatchers.IO) {
            val count = try {
                usageDao.getAllUsage().first().size
            } catch (e: Exception) { 0 }
            
            if (count == 0) {
                val currentData = getUsageData() 
                if (currentData.totalBytesCum > 0) {
                    Log.d(TAG, "Migrating legacy data to Room DB")
                    usageDao.insert(
                         com.airtel.usagetracker.data.db.UsageEntity(
                            timestamp = System.currentTimeMillis(),
                            txBytes = currentData.lastTx,
                            rxBytes = currentData.lastRx,
                            totalBytes = currentData.totalBytesCum,
                            uptimeSeconds = currentData.lastUptime
                        )
                    )
                }
            }
        }
    }
    
    private fun saveUsageData(data: UsageData) {
        prefs.edit().apply {
            putLong("last_tx", data.lastTx)
            putLong("last_rx", data.lastRx)
            putLong("last_uptime", data.lastUptime)
            putLong("total_bytes_cum", data.totalBytesCum)
            putString("last_updated", data.lastUpdated)
            apply()
        }
    }
    
    fun getRouterConfig(): RouterConfig {
        return RouterConfig(
            routerIp = prefs.getString("router_ip", BuildConfig.DEFAULT_ROUTER_IP) ?: BuildConfig.DEFAULT_ROUTER_IP,
            username = prefs.getString("username", BuildConfig.DEFAULT_ROUTER_USERNAME) ?: BuildConfig.DEFAULT_ROUTER_USERNAME,
            password = prefs.getString("password", BuildConfig.DEFAULT_ROUTER_PASSWORD) ?: BuildConfig.DEFAULT_ROUTER_PASSWORD,
            fupLimitGb = prefs.getInt("fup_limit_gb", 3333)
        )
    }
    
    fun saveRouterConfig(config: RouterConfig) {
        prefs.edit().apply {
            putString("router_ip", config.routerIp)
            putString("username", config.username)
            putString("password", config.password)
            putInt("fup_limit_gb", config.fupLimitGb)
            apply()
        }
    }
    
    private var lastDebugInfo = com.airtel.usagetracker.data.models.DebugInfo()
    
    fun getDebugInfo(): com.airtel.usagetracker.data.models.DebugInfo = lastDebugInfo
    
    suspend fun fetchAndUpdateUsage(): Result<UsageData> = withContext(Dispatchers.IO) {
        try {
            // Try migration first just in case it was missed
            migrateLegacyData()
            
            val config = getRouterConfig()
            val previousData = getUsageData()
            
            Log.d(TAG, "Fetching router data...")
            _scrapingStatus.value = ScrapingStatus.LOADING_PAGE
            
            val scrapedData = scraper.scrapeRouterData(
                config.routerIp,
                config.username,
                config.password
            ) { status ->
                // Status callback from scraper
                _scrapingStatus.value = status
            }
            
            _scrapingStatus.value = ScrapingStatus.PARSING
            Log.d(TAG, "Scraped: TX=${scrapedData.tx}, RX=${scrapedData.rx}, Uptime=${scrapedData.uptime}s")
            
            val updatedData = updateUsageData(scrapedData, previousData)
            saveUsageData(updatedData)
            
            // Insert into Room DB
            usageDao.insert(
                com.airtel.usagetracker.data.db.UsageEntity(
                    timestamp = System.currentTimeMillis(),
                    txBytes = scrapedData.tx,
                    rxBytes = scrapedData.rx,
                    totalBytes = updatedData.totalBytesCum,
                    uptimeSeconds = scrapedData.uptime
                )
            )
            
            Log.d(TAG, "Updated usage: ${updatedData.toGigabytes()} GB")
            
            // Update debug info
            val rebootDetected = scrapedData.uptime < previousData.lastUptime
            val deltaTx = if (rebootDetected) 0 else scrapedData.tx - previousData.lastTx
            val deltaRx = if (rebootDetected) 0 else scrapedData.rx - previousData.lastRx
            
            lastDebugInfo = com.airtel.usagetracker.data.models.DebugInfo(
                scrapedTx = scrapedData.tx,
                scrapedRx = scrapedData.rx,
                scrapedUptime = scrapedData.uptime,
                previousTx = previousData.lastTx,
                previousRx = previousData.lastRx,
                previousUptime = previousData.lastUptime,
                deltaTx = deltaTx,
                deltaRx = deltaRx,
                cumulativeBytes = updatedData.totalBytesCum,
                rebootDetected = rebootDetected,
                lastFetchTime = getCurrentTimestamp(),
                lastError = null
            )
            
            Result.success(updatedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching usage", e)
            lastDebugInfo = lastDebugInfo.copy(
                lastError = e.message ?: "Unknown error",
                lastFetchTime = getCurrentTimestamp()
            )
            Result.failure(e)
        }
    }
    
    private fun updateUsageData(current: ScrapedData, previous: UsageData): UsageData {
        val rebootDetected = current.uptime < previous.lastUptime
        
        return if (rebootDetected) {
            Log.d(TAG, "⚠️ REBOOT DETECTED! (Uptime decreased)")
            Log.d(TAG, "   Previous uptime: ${previous.lastUptime}s, Current: ${current.uptime}s")
            Log.d(TAG, "   Pre-reboot counters: TX=${previous.lastTx}, RX=${previous.lastRx}")
            Log.d(TAG, "   Post-reboot counters: TX=${current.tx}, RX=${current.rx}")
            Log.d(TAG, "   → No data added (cumulative already up-to-date)")
            
            // Don't add anything - cumulative is already correct
            // Just reset baseline to current values
            previous.copy(
                lastTx = current.tx,
                lastRx = current.rx,
                lastUptime = current.uptime,
                lastUpdated = getCurrentTimestamp()
            )
        } else {
            // Normal operation: calculate delta and add to cumulative
            val deltaTx = current.tx - previous.lastTx
            val deltaRx = current.rx - previous.lastRx
            
            Log.d(TAG, "   Delta: TX=$deltaTx, RX=$deltaRx bytes")
            
            previous.copy(
                totalBytesCum = previous.totalBytesCum + deltaTx + deltaRx,
                lastTx = current.tx,
                lastRx = current.rx,
                lastUptime = current.uptime,
                lastUpdated = getCurrentTimestamp()
            )
        }
    }
    
    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
