package com.airtel.usagetracker.data.models

import java.time.LocalDate

data class DailyUsage(
    val date: LocalDate,
    val totalBytes: Long,
    val txBytes: Long,
    val rxBytes: Long,
    val recordCount: Int
) {
    fun toGigabytes(): Double = totalBytes / (1024.0 * 1024.0 * 1024.0)
}
