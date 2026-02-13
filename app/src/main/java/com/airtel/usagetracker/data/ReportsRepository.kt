package com.airtel.usagetracker.data

import android.content.Context
import android.util.Log
import com.airtel.usagetracker.data.db.AppDatabase
import com.airtel.usagetracker.data.db.UsageEntity
import com.airtel.usagetracker.data.models.*
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
            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val records = usageDao.getUsageInRangeList(startMillis, endMillis)
            aggregateToDailyUsage(records)
        }

    /**
     * Get weekly digest for a specific week
     */
    suspend fun getWeeklyDigest(weekStart: LocalDate): WeeklyDigest = withContext(Dispatchers.IO) {
        val weekEnd = weekStart.plusDays(6)
        val dailyUsages = getDailyUsages(weekStart, weekEnd)

        val totalUsageGb = dailyUsages.sumOf { it.toGigabytes() }
        val dailyAverageGb = if (dailyUsages.isNotEmpty()) totalUsageGb / dailyUsages.size else 0.0
        
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
        val topRecords = usageDao.getTopUsageDays(limit)
        aggregateToDailyUsage(topRecords)
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
        val allRecords = usageDao.getAllUsageFlow().map { records ->
            aggregateToDailyUsage(records)
        }
        
        outputStream.bufferedWriter().use { writer ->
            writer.write("Date,Total (GB),Upload (GB),Download (GB),Records\n")
            allRecords.collect { dailyUsages ->
                dailyUsages.forEach { usage ->
                    writer.write("${usage.date},${String.format("%.2f", usage.toGigabytes())},")
                    writer.write("${String.format("%.2f", usage.txBytes / (1024.0 * 1024.0 * 1024.0))},")
                    writer.write("${String.format("%.2f", usage.rxBytes / (1024.0 * 1024.0 * 1024.0))},")
                    writer.write("${usage.recordCount}\n")
                }
            }
        }
    }

    /**
     * Export data to JSON format
     */
    suspend fun exportToJson(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val config = usageRepository.getRouterConfig()
        val (cycleStart, cycleEnd) = getBillingCycleRange(LocalDate.now(), config.billingCycleStartDay)
        val dailyUsages = getDailyUsages(cycleStart, cycleEnd)
        
        outputStream.bufferedWriter().use { writer ->
            writer.write("{\n")
            writer.write("  \"exportDate\": \"${LocalDate.now()}\",\n")
            writer.write("  \"billingCycle\": {\n")
            writer.write("    \"start\": \"$cycleStart\",\n")
            writer.write("    \"end\": \"$cycleEnd\"\n")
            writer.write("  },\n")
            writer.write("  \"dailyUsage\": [\n")
            
            dailyUsages.forEachIndexed { index, usage ->
                writer.write("    {\n")
                writer.write("      \"date\": \"${usage.date}\",\n")
                writer.write("      \"totalGb\": ${String.format("%.2f", usage.toGigabytes())},\n")
                writer.write("      \"uploadGb\": ${String.format("%.2f", usage.txBytes / (1024.0 * 1024.0 * 1024.0))},\n")
                writer.write("      \"downloadGb\": ${String.format("%.2f", usage.rxBytes / (1024.0 * 1024.0 * 1024.0))},\n")
                writer.write("      \"records\": ${usage.recordCount}\n")
                writer.write("    }${if (index < dailyUsages.size - 1) "," else ""}\n")
            }
            
            writer.write("  ]\n")
            writer.write("}\n")
        }
    }

    /**
     * Export data to PDF format (placeholder - will implement with iText)
     */
    suspend fun exportToPdf(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        // TODO: Implement PDF generation with iText
        Log.w(TAG, "PDF export not yet implemented")
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
     * Aggregate raw usage records into daily summaries
     */
    private fun aggregateToDailyUsage(records: List<UsageEntity>): List<DailyUsage> {
        return records
            .groupBy { record ->
                Instant.ofEpochMilli(record.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .map { (date, dayRecords) ->
                DailyUsage(
                    date = date,
                    totalBytes = dayRecords.maxOfOrNull { it.totalBytes } ?: 0L,
                    txBytes = dayRecords.maxOfOrNull { it.txBytes } ?: 0L,
                    rxBytes = dayRecords.maxOfOrNull { it.rxBytes } ?: 0L,
                    recordCount = dayRecords.size
                )
            }
            .sortedBy { it.date }
    }
}
