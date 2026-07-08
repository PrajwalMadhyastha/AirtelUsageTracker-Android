package com.prajwal.utilities.tools.wealthtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwal.utilities.tools.wealthtracker.data.CalculatorSettings
import com.prajwal.utilities.tools.wealthtracker.data.WealthPreferences
import com.prajwal.utilities.tools.wealthtracker.data.WealthRepository
import com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity
import com.prajwal.utilities.tools.wealthtracker.data.db.HoldingEntity
import com.prajwal.utilities.tools.wealthtracker.data.network.AssetSearchResult
import com.prajwal.utilities.tools.wealthtracker.data.network.MarketDataRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.prajwal.utilities.tools.wealthtracker.data.WealthBackup
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WealthTrackerViewModel(
    private val repository: WealthRepository,
    private val marketDataRepository: MarketDataRepository,
    private val prefs: WealthPreferences
) : ViewModel() {

    val holdings: StateFlow<List<HoldingEntity>> = repository.getAllHoldings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _searchIsMf = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<AssetSearchResult>> = _searchQuery
        .debounce(500)
        .flatMapLatest { query ->
            if (query.length < 2) {
                flowOf(emptyList())
            } else {
                flow {
                    emit(marketDataRepository.searchAsset(query, _searchIsMf.value))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String, isMf: Boolean) {
        _searchIsMf.value = isMf
        _searchQuery.value = query
    }

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

    val isBiometricEnabled: StateFlow<Boolean> = prefs.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun setAuthenticated(auth: Boolean) {
        _isAuthenticated.value = auth
    }

    fun toggleBiometric() {
        viewModelScope.launch {
            prefs.updateBiometricEnabled(!isBiometricEnabled.value)
        }
    }

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

    /** Holdings management */
    fun addHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            repository.insertHolding(holding)
            syncPricesNow()
        }
    }

    fun updateHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            repository.updateHolding(holding)
            syncPricesNow()
        }
    }

    fun deleteHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            repository.deleteHolding(holding)
        }
    }

    fun syncPricesNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                // FIX #1: Use the already-subscribed `holdings` StateFlow directly.
                // The previous code called .stateIn(viewModelScope).value on a cold Flow,
                // which always returned an empty list before the first DB emission arrived.
                val currentHoldings = holdings.value
                for (holding in currentHoldings) {
                    val prices = if (holding.instrumentType == "MF") {
                        marketDataRepository.fetchMfNav(holding.identifier)
                    } else {
                        marketDataRepository.fetchStockPrice(holding.identifier, holding.exchange)
                    }

                    if (prices != null) {
                        repository.updateHoldingPrice(
                            id = holding.id,
                            price = prices.latestPrice,
                            previousClosePrice = prices.previousClosePrice
                        )
                    }
                }
            } finally {
                _isSyncing.value = false
            }
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

    fun exportData(uri: Uri, contentResolver: ContentResolver, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentHoldings = holdings.value
                val currentSnapshots = snapshots.value
                val backup = WealthBackup(currentHoldings, currentSnapshots)
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val json = moshi.adapter(WealthBackup::class.java).toJson(backup)
                
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                Log.e("WealthTracker", "Error exporting data", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun importData(uri: Uri, contentResolver: ContentResolver, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (json != null) {
                    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                    val backup = moshi.adapter(WealthBackup::class.java).fromJson(json)
                    if (backup != null) {
                        backup.holdings.forEach { repository.insertHolding(it.copy(id = 0)) }
                        backup.snapshots.forEach { repository.insertSnapshot(it.copy(id = 0)) }
                        syncPricesNow()
                        withContext(Dispatchers.Main) { onComplete(true) }
                        return@launch
                    }
                }
                withContext(Dispatchers.Main) { onComplete(false) }
            } catch (e: Exception) {
                Log.e("WealthTracker", "Error importing data", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }
}
