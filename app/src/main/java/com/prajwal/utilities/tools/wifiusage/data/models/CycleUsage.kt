package com.prajwal.utilities.tools.wifiusage.data.models

import java.time.LocalDate

data class CycleUsage(
    val cycleStart: LocalDate,
    val cycleEnd: LocalDate,
    val totalUsageGb: Double,
    val dailyAverageGb: Double,
    val peakDayUsageGb: Double,
    val daysInCycle: Int
)
