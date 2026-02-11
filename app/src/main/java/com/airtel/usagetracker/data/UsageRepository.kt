package com.airtel.usagetracker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.airtel.usagetracker.BuildConfig
import com.airtel.usagetracker.data.models.RouterConfig
import com.airtel.usagetracker.data.models.ScrapedData
import com.airtel.usagetracker.data.models.UsageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageRepository(private val context: Context) {
    
    private val TAG = "UsageRepository"
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("usage_data", Context.MODE_PRIVATE)
    
    private val scraper = RouterScraper(context)
    
    fun getUsageData(): UsageData {
        return UsageData(
            lastTx = prefs.getLong("last_tx", 0),
            lastRx = prefs.getLong("last_rx", 0),
            lastUptime = prefs.getLong("last_uptime", 0),
            totalBytesCum = prefs.getLong("total_bytes_cum", 0),
            lastUpdated = prefs.getString("last_updated", "") ?: ""
        )
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
    
    suspend fun fetchAndUpdateUsage(): Result<UsageData> = withContext(Dispatchers.IO) {
        try {
            val config = getRouterConfig()
            val previousData = getUsageData()
            
            Log.d(TAG, "Fetching router data...")
            val scrapedData = scraper.scrapeRouterData(
                config.routerIp,
                config.username,
                config.password
            )
            
            Log.d(TAG, "Scraped: TX=${scrapedData.tx}, RX=${scrapedData.rx}, Uptime=${scrapedData.uptime}s")
            
            val updatedData = updateUsageData(scrapedData, previousData)
            saveUsageData(updatedData)
            
            Log.d(TAG, "Updated usage: ${updatedData.toGigabytes()} GB")
            
            Result.success(updatedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching usage", e)
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
}
