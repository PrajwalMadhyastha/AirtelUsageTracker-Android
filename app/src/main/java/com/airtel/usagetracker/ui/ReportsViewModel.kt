package com.airtel.usagetracker.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airtel.usagetracker.data.ReportsRepository
import com.airtel.usagetracker.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReportsViewModel(private val reportsRepository: ReportsRepository) : ViewModel() {

    private val TAG = "ReportsViewModel"

    // Weekly Digest
    private val _weeklyDigest = MutableStateFlow<WeeklyDigest?>(null)
    val weeklyDigest: StateFlow<WeeklyDigest?> = _weeklyDigest.asStateFlow()

    // FUP Projection
    private val _fupProjection = MutableStateFlow<FupProjection?>(null)
    val fupProjection: StateFlow<FupProjection?> = _fupProjection.asStateFlow()

    // Timeline Data
    private val _timelineData = MutableStateFlow<List<DailyUsage>>(emptyList())
    val timelineData: StateFlow<List<DailyUsage>> = _timelineData.asStateFlow()

    private val _selectedTimePeriod = MutableStateFlow(TimePeriod.WEEK)
    val selectedTimePeriod: StateFlow<TimePeriod> = _selectedTimePeriod.asStateFlow()

    // Cycle Filter
    private val _availableCycles = MutableStateFlow<List<CycleUsage>>(emptyList())
    val availableCycles: StateFlow<List<CycleUsage>> = _availableCycles.asStateFlow()
    
    private val _selectedCycle = MutableStateFlow<CycleUsage?>(null)
    val selectedCycle: StateFlow<CycleUsage?> = _selectedCycle.asStateFlow()
    
    // Cycle Trends
    private val _cycleUsages = MutableStateFlow<List<CycleUsage>>(emptyList())
    val cycleUsages: StateFlow<List<CycleUsage>> = _cycleUsages.asStateFlow()

    // Top Usage Days
    private val _topUsageDays = MutableStateFlow<List<DailyUsage>>(emptyList())
    val topUsageDays: StateFlow<List<DailyUsage>> = _topUsageDays.asStateFlow()

    // Monthly Comparison
    private val _monthlyComparison = MutableStateFlow<MonthlyComparison?>(null)
    val monthlyComparison: StateFlow<MonthlyComparison?> = _monthlyComparison.asStateFlow()

    // Calendar Data
    private val _calendarMonth = MutableStateFlow(LocalDate.now())
    val calendarMonth: StateFlow<LocalDate> = _calendarMonth.asStateFlow()

    private val _calendarData = MutableStateFlow<List<DailyUsage>>(emptyList())
    val calendarData: StateFlow<List<DailyUsage>> = _calendarData.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refreshReports()
    }

    fun refreshReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Load weekly digest
                val currentWeekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                _weeklyDigest.value = reportsRepository.getWeeklyDigest(currentWeekStart)

                // Load FUP projection
                _fupProjection.value = reportsRepository.getFupProjection()

                // Load all available cycles for filter
                val allCycles = reportsRepository.getCycleUsages(12)
                _availableCycles.value = allCycles
                
                // Load cycle trends (last 6 cycles)
                _cycleUsages.value = reportsRepository.getCycleUsages(6)

                // Load monthly comparison
                _monthlyComparison.value = reportsRepository.getMonthlyComparison()

                // Determine effective cycle (preserve selection or default to first)
                val effectiveCycle = _selectedCycle.value ?: allCycles.firstOrNull()
                
                // Update _selectedCycle to match effective cycle if needed
                if (_selectedCycle.value == null) {
                    _selectedCycle.value = effectiveCycle
                }
                
                // Load data for the effective cycle
                loadCycleData(effectiveCycle)

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load reports"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTimePeriod(period: TimePeriod) {
        _selectedTimePeriod.value = period
        viewModelScope.launch {
            updateTimelineData()
        }
    }

    private suspend fun updateTimelineData() {
        val today = LocalDate.now()
        val (startDate, endDate) = when (_selectedTimePeriod.value) {
            TimePeriod.TODAY -> today to today
            TimePeriod.WEEK -> {
                val weekStart = today.minusDays(6)
                weekStart to today
            }
            TimePeriod.MONTH -> {
                val monthStart = today.withDayOfMonth(1)
                monthStart to today
            }
            TimePeriod.CYCLE -> {
                // Get current billing cycle dates from repository
                val projection = reportsRepository.getFupProjection()
                val cycleStart = today.minusDays(projection.daysElapsed.toLong() - 1)
                cycleStart to today
            }
            TimePeriod.ALL_TIME -> {
                // Get earliest record date
                LocalDate.of(2020, 1, 1) to today
            }
        }

        _timelineData.value = reportsRepository.getDailyUsages(startDate, endDate)
    }

    fun navigateCalendarMonth(offset: Int) {
        _calendarMonth.value = _calendarMonth.value.plusMonths(offset.toLong())
        viewModelScope.launch {
            updateCalendarData()
        }
    }


    fun selectCycle(cycle: CycleUsage?) {
        _selectedCycle.value = cycle
        viewModelScope.launch {
            loadCycleData(cycle)
        }
    }

    private suspend fun loadCycleData(cycle: CycleUsage?) {
        if (cycle != null) {
            // Update timeline data to show only this cycle
            _timelineData.value = reportsRepository.getDailyUsages(cycle.cycleStart, cycle.cycleEnd)
            
            // Update top usage days for this cycle
            val cycleData = reportsRepository.getDailyUsages(cycle.cycleStart, cycle.cycleEnd)
            _topUsageDays.value = cycleData.sortedByDescending { it.totalBytes }.take(10)
            
            // Update calendar to show this cycle's month
            _calendarMonth.value = cycle.cycleStart
            updateCalendarData()
        } else {
            // Show all data
            updateTimelineData()
            _topUsageDays.value = reportsRepository.getTopUsageDays(10)
        }
    }

    private suspend fun updateCalendarData() {
        val month = _calendarMonth.value
        val startDate = month.withDayOfMonth(1)
        val endDate = month.withDayOfMonth(month.lengthOfMonth())
        _calendarData.value = reportsRepository.getDailyUsages(startDate, endDate)
    }

    fun exportData(format: ExportFormat, uri: Uri, contentResolver: android.content.ContentResolver) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    when (format) {
                        ExportFormat.CSV -> reportsRepository.exportToCsv(outputStream)
                        ExportFormat.JSON -> reportsRepository.exportToJson(outputStream)
                        ExportFormat.PDF -> reportsRepository.exportToPdf(outputStream)
                        else -> throw IllegalArgumentException("Unsupported format")
                    }
                }
                _errorMessage.value = "Export successful"
            } catch (e: Exception) {
                _errorMessage.value = "Export failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
