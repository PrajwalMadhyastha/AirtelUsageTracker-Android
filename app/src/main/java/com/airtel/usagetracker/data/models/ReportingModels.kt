package com.airtel.usagetracker.data.models

enum class TimePeriod(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    CYCLE("Cycle"),
    ALL_TIME("All Time")
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
