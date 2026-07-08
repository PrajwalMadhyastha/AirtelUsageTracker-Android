package com.prajwal.utilities.tools.wealthtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwal.utilities.tools.wealthtracker.data.CalculatorSettings
import com.prajwal.utilities.tools.wealthtracker.data.WealthPreferences
import com.prajwal.utilities.tools.wealthtracker.data.WealthRepository
import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WealthTrackerViewModel(
    private val repository: WealthRepository,
    private val prefs: WealthPreferences
) : ViewModel() {

    /** All snapshots newest-first (for history list + latest diversification). */
    val snapshots: StateFlow<List<AssetSnapshotEntity>> = repository.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** All snapshots oldest-first (for growth chart). */
    val snapshotsChronological: StateFlow<List<AssetSnapshotEntity>> =
        repository.getAllSnapshotsChronological()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Combined calculator settings as a single object. */
    val calculatorSettings: StateFlow<CalculatorSettings> = combine(
        prefs.monthlyInvestment,
        prefs.annualStepupPercent,
        prefs.expectedReturnPercent
    ) { monthly, stepup, returns ->
        CalculatorSettings(monthly, stepup, returns)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalculatorSettings()
    )

    /** Tracks if the "pre-fill from latest snapshot" operation is loading. */
    private val _prefillSnapshot = MutableStateFlow<AssetSnapshotEntity?>(null)
    val prefillSnapshot: StateFlow<AssetSnapshotEntity?> = _prefillSnapshot.asStateFlow()

    /** Save a new portfolio snapshot (called when user taps "Save Snapshot"). */
    fun saveSnapshot(snapshot: AssetSnapshotEntity) {
        viewModelScope.launch {
            repository.insertSnapshot(snapshot)
        }
    }

    /** Delete a snapshot from history. */
    fun deleteSnapshot(snapshot: AssetSnapshotEntity) {
        viewModelScope.launch {
            repository.deleteSnapshot(snapshot)
        }
    }

    /** Load the latest snapshot to pre-fill the portfolio form. */
    fun loadPrefill() {
        viewModelScope.launch {
            _prefillSnapshot.value = repository.getLatestSnapshot()
        }
    }

    /** Update monthly SIP setting. */
    fun updateMonthlyInvestment(amount: Double) {
        viewModelScope.launch { prefs.updateMonthlyInvestment(amount) }
    }

    /** Update annual step-up % setting. */
    fun updateAnnualStepup(percent: Double) {
        viewModelScope.launch { prefs.updateAnnualStepup(percent) }
    }

    /** Update expected annual return % setting. */
    fun updateExpectedReturn(percent: Double) {
        viewModelScope.launch { prefs.updateExpectedReturn(percent) }
    }
}
