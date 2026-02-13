package com.airtel.usagetracker.data.models

enum class TimePeriod {
    TODAY,
    WEEK,
    MONTH,
    CYCLE,
    ALL_TIME
}

enum class ExportFormat {
    CSV,
    JSON,
    PDF,
    IMAGE
}

data class MonthlyComparison(
    val currentMonth: CycleUsage,
    val previousMonth: CycleUsage,
    val differenceGb: Double,
    val percentageChange: Double
)
