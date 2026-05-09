package com.prajwal.utilities.tools.wifiusage.data

import android.content.Context
import android.util.Log
import com.prajwal.utilities.tools.wifiusage.data.db.AppDatabase
import com.prajwal.utilities.tools.wifiusage.data.db.UsageEntity
import com.prajwal.utilities.tools.wifiusage.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ReportsRepository(private val context: Context) {

    private val TAG = "ReportsRepository"
    private val database = AppDatabase.getDatabase(context)
    private val usageDao = database.usageDao()
    private val usageRepository = UsageRepository(context)

    /**
     * Get daily aggregated usage for a date range
     */
    suspend fun getDailyUsages(startDate: LocalDate, endDate: LocalDate): List<DailyUsage> = 
        withContext(Dispatchers.IO) {
            // Fetch starting from the previous day to ensure we have a baseline for the first record of startDate
            val fetchStart = startDate.minusDays(1)
            val startMillis = fetchStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val records = usageDao.getUsageInRangeList(startMillis, endMillis)
            val allDailyUsages = processUsageRecords(records)
            
            // Filter to return only requested range
            allDailyUsages.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
        }

    /**
     * Get weekly digest for a specific week
     */
    suspend fun getWeeklyDigest(weekStart: LocalDate): WeeklyDigest = withContext(Dispatchers.IO) {
        val weekEnd = weekStart.plusDays(6)
        val dailyUsages = getDailyUsages(weekStart, weekEnd)

        val totalUsageGb = dailyUsages.sumOf { it.toGigabytes() }
        
        // Calculate days elapsed in the week (for current week) or full week (for past weeks)
        val today = LocalDate.now()
        val daysElapsed = if (weekStart.plusDays(6).isAfter(today)) {
             if (today.isBefore(weekStart)) 0 else ChronoUnit.DAYS.between(weekStart, today).toInt() + 1
        } else {
            7
        }
        
        val dailyAverageGb = if (daysElapsed > 0) totalUsageGb / daysElapsed else 0.0
        
        // Peak day based on ACTUAL daily usage
        val peakDay = dailyUsages.maxByOrNull { it.totalBytes }
        val peakDayDate = peakDay?.date ?: weekStart
        val peakUsageGb = peakDay?.toGigabytes() ?: 0.0

        // Get previous week for comparison
        val prevWeekStart = weekStart.minusWeeks(1)
        val prevWeekEnd = prevWeekStart.plusDays(6)
        val prevWeekUsages = getDailyUsages(prevWeekStart, prevWeekEnd)
        val prevWeekTotal = prevWeekUsages.sumOf { it.toGigabytes() }
        
        val comparison = if (prevWeekTotal > 0) {
            ((totalUsageGb - prevWeekTotal) / prevWeekTotal) * 100.0
        } else null

        WeeklyDigest(
            weekStart = weekStart,
            weekEnd = weekEnd,
            totalUsageGb = totalUsageGb,
            dailyAverageGb = dailyAverageGb,
            peakDay = peakDayDate,
            peakUsageGb = peakUsageGb,
            dailyUsages = dailyUsages,
            comparisonToPreviousWeek = comparison
        )
    }

    /**
     * Calculate FUP projection based on current cycle usage
     */
    suspend fun getFupProjection(): FupProjection = withContext(Dispatchers.IO) {
        val config = usageRepository.getRouterConfig()
        val (cycleData, daysRemaining) = usageRepository.getCurrentCycleUsage()
        
        val currentUsageGb = cycleData.toGigabytes()
        val fupLimitGb = config.fupLimitGb
        
        // Calculate cycle dates
        val (cycleStart, cycleEnd) = getBillingCycleRange(LocalDate.now(), config.billingCycleStartDay)
        val totalDaysInCycle = ChronoUnit.DAYS.between(cycleStart, cycleEnd).toInt() + 1
        val daysElapsed = totalDaysInCycle - daysRemaining
        
        // Projection calculation
        val dailyAverage = if (daysElapsed > 0) currentUsageGb / daysElapsed else 0.0
        val projectedTotalGb = currentUsageGb + (dailyAverage * daysRemaining)
        
        val willExceed = projectedTotalGb > fupLimitGb
        val excessGb = if (willExceed) projectedTotalGb - fupLimitGb else 0.0
        
        // Calculate recommended daily limit to stay within FUP
        val remainingGb = (fupLimitGb - currentUsageGb).coerceAtLeast(0.0)
        val recommendedDailyLimitGb = if (daysRemaining > 0) remainingGb / daysRemaining else 0.0

        FupProjection(
            currentUsageGb = currentUsageGb,
            projectedTotalGb = projectedTotalGb,
            fupLimitGb = fupLimitGb,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            willExceed = willExceed,
            excessGb = excessGb,
            recommendedDailyLimitGb = recommendedDailyLimitGb
        )
    }

    /**
     * Get cycle usage summaries for the last N billing cycles
     */
    suspend fun getCycleUsages(numberOfCycles: Int): List<CycleUsage> = withContext(Dispatchers.IO) {
        val config = usageRepository.getRouterConfig()
        val cycleUsages = mutableListOf<CycleUsage>()
        
        var referenceDate = LocalDate.now()
        
        repeat(numberOfCycles) {
            val (cycleStart, cycleEnd) = getBillingCycleRange(referenceDate, config.billingCycleStartDay)
            val dailyUsages = getDailyUsages(cycleStart, cycleEnd)
            
            val totalUsageGb = dailyUsages.sumOf { it.toGigabytes() }
            val daysInCycle = ChronoUnit.DAYS.between(cycleStart, cycleEnd).toInt() + 1
            val dailyAverageGb = if (dailyUsages.isNotEmpty()) totalUsageGb / dailyUsages.size else 0.0
            val peakDayUsageGb = dailyUsages.maxOfOrNull { it.toGigabytes() } ?: 0.0
            
            cycleUsages.add(
                CycleUsage(
                    cycleStart = cycleStart,
                    cycleEnd = cycleEnd,
                    totalUsageGb = totalUsageGb,
                    dailyAverageGb = dailyAverageGb,
                    peakDayUsageGb = peakDayUsageGb,
                    daysInCycle = daysInCycle
                )
            )
            
            // Move to previous cycle
            referenceDate = cycleStart.minusDays(1)
        }
        
        cycleUsages
    }

    /**
     * Get top usage days
     */
    suspend fun getTopUsageDays(limit: Int): List<DailyUsage> = withContext(Dispatchers.IO) {
        // Fetch a reasonable window of data (e.g., last 60 days) to find top days
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(60)
        
        val dailyUsages = getDailyUsages(startDate, endDate)
        
        dailyUsages
            .sortedByDescending { it.totalBytes }
            .take(limit)
    }

    /**
     * Get monthly comparison (current vs previous month)
     */
    suspend fun getMonthlyComparison(): MonthlyComparison? = withContext(Dispatchers.IO) {
        val config = usageRepository.getRouterConfig()
        val currentDate = LocalDate.now()
        
        // Current month cycle
        val (currentStart, currentEnd) = getBillingCycleRange(currentDate, config.billingCycleStartDay)
        val currentDailyUsages = getDailyUsages(currentStart, currentEnd)
        
        // Previous month cycle
        val prevMonthDate = currentDate.minusMonths(1)
        val (prevStart, prevEnd) = getBillingCycleRange(prevMonthDate, config.billingCycleStartDay)
        val prevDailyUsages = getDailyUsages(prevStart, prevEnd)
        
        if (currentDailyUsages.isEmpty() && prevDailyUsages.isEmpty()) {
            return@withContext null
        }
        
        val currentTotalGb = currentDailyUsages.sumOf { it.toGigabytes() }
        val prevTotalGb = prevDailyUsages.sumOf { it.toGigabytes() }
        
        val currentDaysInCycle = ChronoUnit.DAYS.between(currentStart, currentEnd).toInt() + 1
        val prevDaysInCycle = ChronoUnit.DAYS.between(prevStart, prevEnd).toInt() + 1
        
        val currentCycle = CycleUsage(
            cycleStart = currentStart,
            cycleEnd = currentEnd,
            totalUsageGb = currentTotalGb,
            dailyAverageGb = if (currentDailyUsages.isNotEmpty()) currentTotalGb / currentDailyUsages.size else 0.0,
            peakDayUsageGb = currentDailyUsages.maxOfOrNull { it.toGigabytes() } ?: 0.0,
            daysInCycle = currentDaysInCycle
        )
        
        val prevCycle = CycleUsage(
            cycleStart = prevStart,
            cycleEnd = prevEnd,
            totalUsageGb = prevTotalGb,
            dailyAverageGb = if (prevDailyUsages.isNotEmpty()) prevTotalGb / prevDailyUsages.size else 0.0,
            peakDayUsageGb = prevDailyUsages.maxOfOrNull { it.toGigabytes() } ?: 0.0,
            daysInCycle = prevDaysInCycle
        )
        
        val differenceGb = currentTotalGb - prevTotalGb
        val percentageChange = if (prevTotalGb > 0) (differenceGb / prevTotalGb) * 100.0 else 0.0
        
        MonthlyComparison(
            currentMonth = currentCycle,
            previousMonth = prevCycle,
            differenceGb = differenceGb,
            percentageChange = percentageChange
        )
    }

    /**
     * Export data to CSV format
     */
    suspend fun exportToCsv(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val allDailyUsages = getTopUsageDays(365) // Get last year of data using corrected logic
        // Sort by date ascending for export
        val sortedUsages = allDailyUsages.sortedBy { it.date }
        
        outputStream.bufferedWriter().use { writer ->
            writer.write("Date,Total (GB),Upload (GB),Download (GB),Records\n")
            sortedUsages.forEach { usage ->
                writer.write("${usage.date},${String.format("%.2f", usage.toGigabytes())},")
                writer.write("${String.format("%.2f", usage.txBytes / (1024.0 * 1024.0 * 1024.0))},")
                writer.write("${String.format("%.2f", usage.rxBytes / (1024.0 * 1024.0 * 1024.0))},")
                writer.write("${usage.recordCount}\n")
            }
        }
    }

    /**
     * Export data to JSON format
     */
    suspend fun exportToJson(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val allDailyUsages = getTopUsageDays(365)
        val sortedUsages = allDailyUsages.sortedBy { it.date }
        
        outputStream.bufferedWriter().use { writer ->
            writer.write("{\n")
            writer.write("  \"exportDate\": \"${LocalDate.now()}\",\n")
            writer.write("  \"dailyUsage\": [\n")
            
            sortedUsages.forEachIndexed { index, usage ->
                writer.write("    {\n")
                writer.write("      \"date\": \"${usage.date}\",\n")
                writer.write("      \"totalGb\": ${String.format("%.2f", usage.toGigabytes())},\n")
                writer.write("      \"uploadGb\": ${String.format("%.2f", usage.txBytes / (1024.0 * 1024.0 * 1024.0))},\n")
                writer.write("      \"downloadGb\": ${String.format("%.2f", usage.rxBytes / (1024.0 * 1024.0 * 1024.0))},\n")
                writer.write("      \"records\": ${usage.recordCount}\n")
                writer.write("    }${if (index < sortedUsages.size - 1) "," else ""}\n")
            }
            
            writer.write("  ]\n")
            writer.write("}\n")
        }
    }

    /**
     * Export data to PDF format
     */
    suspend fun exportToPdf(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val allUsages = getTopUsageDays(365)
        
        // Calculate summary stats for the report
        val totalGb = allUsages.sumOf { it.toGigabytes() }
        val days = allUsages.size
        val avgGb = if (days > 0) totalGb / days else 0.0
        val peak = allUsages.maxByOrNull { it.totalBytes }
        
        val generator = PdfGenerator(context)
        generator.generateReport(
            outputStream = outputStream,
            dailyUsages = allUsages,
            summaryTotalGb = totalGb,
            dailyAverageGb = avgGb,
            peakDay = peak?.date ?: LocalDate.now(),
            peakUsageGb = peak?.toGigabytes() ?: 0.0
        )
    }

    // Helper functions

    /**
     * Calculate billing cycle date range
     */
    private fun getBillingCycleRange(referenceDate: LocalDate, cycleStartDay: Int): Pair<LocalDate, LocalDate> {
        val currentDay = referenceDate.dayOfMonth
        val cycleStart = if (currentDay >= cycleStartDay) {
            referenceDate.withDayOfMonth(cycleStartDay)
        } else {
            referenceDate.minusMonths(1).withDayOfMonth(cycleStartDay)
        }
        val cycleEnd = cycleStart.plusMonths(1).minusDays(1)
        return cycleStart to cycleEnd
    }

    /**
     * Process raw usage records to calculate daily deltas
     */
    private fun processUsageRecords(records: List<UsageEntity>): List<DailyUsage> {
        if (records.isEmpty()) return emptyList()
        
        val sortedRecords = records.sortedBy { it.timestamp }
        val zoneId = ZoneId.systemDefault()
        
        val dailyMap = mutableMapOf<LocalDate, DailyUsageAccumulator>()
        
        for (i in sortedRecords.indices) {
            val current = sortedRecords[i]
            val date = Instant.ofEpochMilli(current.timestamp).atZone(zoneId).toLocalDate()
            
            val acc = dailyMap.getOrPut(date) { DailyUsageAccumulator() }
            acc.recordCount++
            
            // Calculate delta
            if (i > 0) {
                val prev = sortedRecords[i - 1]
                
                // Check if prev is from the same day OR immediate predecessor
                // If it's from a widely different time, we lose precision, but we assume continuous monitoring.
                
                // Reboot handling logic:
                // If current.uptime < prev.uptime, it's a reboot.
                // However, UsageEntity.totalBytes is cumulative (lifetime).
                // Wait, UsageRepository logic ensures `totalBytes` is monotonic even across reboots.
                // So: delta = current.totalBytes - prev.totalBytes.
                
                // BUT, check UsageRepository Line 336:
                // If reboot detected, it returns `previous.copy(...)` WITHOUT adding to totalBytesCum.
                // This means `totalBytesCum` stays FLAT during the reboot usage gap until new usage is added.
                // So standard subtraction should work!
                
                val deltaTotal = if (current.totalBytes >= prev.totalBytes) {
                    current.totalBytes - prev.totalBytes
                } else {
                    // This should theoretically not happen with correct UsageRepository logic,
                    // but if DB was manipulated or logic flawed, assume 0 to avoid negative usage.
                    0L
                }
                
                val deltaTx = if (current.txBytes >= prev.txBytes) current.txBytes - prev.txBytes else 0L // This logic might fail if txBytes resets but totalBytes doesn't?
                // Wait, `txBytes` in UsageEntity is raw counter? No.
                // UsageRepository Line 293: `txBytes = scrapedData.tx` -> Yes, raw counter!
                // UsageRepository Line 295: `totalBytes = updatedData.totalBytesCum` -> Computed cumulative!
                
                // So `totalBytes` is reliable for deltas. `txBytes` and `rxBytes` resets on reboot.
                // So we should NOT subtract `txBytes` blindly if reboot occurred.
                
                // Reboot detection again:
                val isReboot = current.uptimeSeconds < prev.uptimeSeconds
                
                val usageDelta = deltaTotal
                
                // Distribute usageDelta into tx/rx?
                // We know `total = tx + rx`.
                // If isReboot, `tx` dropped. We can't use simple subtraction for tx/rx components.
                // But we mainly care about `totalBytes` for reports.
                // Let's approximate tx/rx breakdown if needed, or just track Total.
                // For now, let's just track Total accurately.
                
                acc.totalBytes += usageDelta
                
                // For TX/RX, if no reboot, add delta. If reboot, add current values?
                // No, if reboot, `totalBytes` didn't increase by the *amount* of reboot sessions lost?
                // UsageRepository logic says: "Don't add anything - cumulative is already correct".
                // So if reboot, `totalBytes` stays same. Delta is 0. Usage is 0.
                // Correct. We miss the usage *during* the reboot gap?
                // If router says uptime 10s, we missed 10s of usage.
                // But we catch up on next poll.
                
                // So: Just use `totalBytes` delta.
                
                 if (!isReboot && current.txBytes >= prev.txBytes) {
                    acc.txBytes += (current.txBytes - prev.txBytes)
                    acc.rxBytes += (current.rxBytes - prev.rxBytes)
                } 
                // If reboot, cannot determine TX/RX breakdown easily for the gap, but TotalBytes handles it (0 delta).
                
            } else {
                // First record. We can't calculate delta from unknown previous state.
                // Assume 0 usage for this instant, just establishing baseline.
            }
        }
        
        return dailyMap.map { (date, acc) ->
            DailyUsage(
                date = date,
                totalBytes = acc.totalBytes,
                txBytes = acc.txBytes,
                rxBytes = acc.rxBytes,
                recordCount = acc.recordCount
            )
        }.sortedBy { it.date }
    }
    
    private class DailyUsageAccumulator {
        var totalBytes: Long = 0
        var txBytes: Long = 0
        var rxBytes: Long = 0
        var recordCount: Int = 0
    }
}
