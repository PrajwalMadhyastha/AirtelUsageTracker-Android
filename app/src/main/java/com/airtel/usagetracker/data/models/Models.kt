package com.airtel.usagetracker.data.models

data class UsageData(
    val lastTx: Long = 0,
    val lastRx: Long = 0,
    val lastUptime: Long = 0,
    val totalBytesCum: Long = 0,
    val lastUpdated: String = ""
) {
    fun toGigabytes(): Double = totalBytesCum / (1024.0 * 1024.0 * 1024.0)
    
    fun getPercentage(fupLimitGb: Int): Double = 
        (toGigabytes() / fupLimitGb) * 100.0
}

data class ScrapedData(
    val tx: Long,
    val rx: Long,
    val uptime: Long
)

data class RouterConfig(
    val routerIp: String = "192.168.1.1",
    val username: String = "admin",
    val password: String = "admin",
    val fupLimitGb: Int = 3333,
    val billingCycleStartDay: Int = 1
)

data class DebugInfo(
    val scrapedTx: Long = 0,
    val scrapedRx: Long = 0,
    val scrapedUptime: Long = 0,
    val previousTx: Long = 0,
    val previousRx: Long = 0,
    val previousUptime: Long = 0,
    val deltaTx: Long = 0,
    val deltaRx: Long = 0,
    val cumulativeBytes: Long = 0,
    val rebootDetected: Boolean = false,
    val lastFetchTime: String = "",
    val lastError: String? = null
)

enum class ScrapingStatus(val message: String) {
    IDLE("Ready to fetch"),
    CONNECTING("Connecting to router..."),
    LOADING_PAGE("Loading router page..."),
    LOGGING_IN("Logging in..."),
    NAVIGATING("Navigating to data page..."),
    SCRAPING_DATA("Fetching usage data..."),
    PARSING("Processing data..."),
    SUCCESS("Data fetched successfully"),
    ERROR("Failed to fetch data")
}
