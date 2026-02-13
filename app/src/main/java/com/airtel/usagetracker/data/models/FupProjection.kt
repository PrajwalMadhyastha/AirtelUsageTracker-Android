package com.airtel.usagetracker.data.models

data class FupProjection(
    val currentUsageGb: Double,
    val projectedTotalGb: Double,
    val fupLimitGb: Int,
    val daysElapsed: Int,
    val daysRemaining: Int,
    val willExceed: Boolean,
    val excessGb: Double,
    val recommendedDailyLimitGb: Double
)
