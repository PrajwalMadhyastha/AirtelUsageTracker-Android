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
    val fupLimitGb: Int = 3333
)
