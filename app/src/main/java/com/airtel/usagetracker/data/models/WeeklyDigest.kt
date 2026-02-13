package com.airtel.usagetracker.data.models

import java.time.LocalDate

data class WeeklyDigest(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalUsageGb: Double,
    val dailyAverageGb: Double,
    val peakDay: LocalDate,
    val peakUsageGb: Double,
    val dailyUsages: List<DailyUsage>,
    val comparisonToPreviousWeek: Double? = null
)
