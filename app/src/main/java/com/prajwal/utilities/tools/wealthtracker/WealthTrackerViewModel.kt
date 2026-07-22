package com.prajwal.utilities.tools.wealthtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prajwal.utilities.tools.wealthtracker.data.CalculatorSettings
import com.prajwal.utilities.tools.wealthtracker.data.SortOption
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
import kotlinx.coroutines.flow.first
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

    val transactions: StateFlow<List<com.prajwal.utilities.tools.wealthtracker.data.db.TransactionEntity>> = repository.getAllTransactions()
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

    val holdingsSortOption: StateFlow<SortOption> = prefs.holdingsSortOption
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.DEFAULT)

    val holdingsSortAscending: StateFlow<Boolean> = prefs.holdingsSortAscending
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onSortChipClicked(option: SortOption) {
        viewModelScope.launch {
            val currentOption = prefs.holdingsSortOption.first()
            val defaultAsc = option == SortOption.ALPHABETICAL || option == SortOption.DEFAULT
            
            if (currentOption == option) {
                val currentAsc = prefs.holdingsSortAscending.first()
                if (currentAsc == defaultAsc) {
                    prefs.updateHoldingsSortAscending(!defaultAsc)
                } else {
                    prefs.updateHoldingsSortOption(SortOption.DEFAULT)
                    prefs.updateHoldingsSortAscending(true)
                }
            } else {
                prefs.updateHoldingsSortOption(option)
                prefs.updateHoldingsSortAscending(defaultAsc)
            }
        }
    }



    fun toggleBiometric() {
        viewModelScope.launch {
            prefs.updateBiometricEnabled(!isBiometricEnabled.value)
        }
    }

    val autoSnapshotEnabled: StateFlow<Boolean> = prefs.autoSnapshotEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoSnapshotDayOfMonth: StateFlow<Int> = prefs.autoSnapshotDayOfMonth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun updateAutoSnapshotEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.updateAutoSnapshotEnabled(enabled) }
    }

    fun updateAutoSnapshotDayOfMonth(day: Int) {
        viewModelScope.launch { prefs.updateAutoSnapshotDayOfMonth(day) }
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
            val id = repository.insertHolding(holding)
            
            // Log initial BUY transaction for Day Zero tracking
            if (holding.unitsHeld > 0) {
                val pricePerUnit = holding.investedAmount / holding.unitsHeld
                repository.insertTransaction(
                    com.prajwal.utilities.tools.wealthtracker.data.db.TransactionEntity(
                        holdingId = id.toInt(),
                        timestamp = System.currentTimeMillis(),
                        units = holding.unitsHeld,
                        pricePerUnit = pricePerUnit,
                        type = com.prajwal.utilities.tools.wealthtracker.data.db.TransactionType.BUY
                    )
                )
            }
            
            syncPricesNow()
        }
    }

    fun topUpHolding(holding: HoldingEntity, addedUnits: Double, addedInvested: Double) {
        viewModelScope.launch {
            val updatedHolding = holding.copy(
                unitsHeld = holding.unitsHeld + addedUnits,
                investedAmount = holding.investedAmount + addedInvested
            )
            repository.updateHolding(updatedHolding)
            
            if (addedUnits > 0) {
                val pricePerUnit = addedInvested / addedUnits
                repository.insertTransaction(
                    com.prajwal.utilities.tools.wealthtracker.data.db.TransactionEntity(
                        holdingId = holding.id,
                        timestamp = System.currentTimeMillis(),
                        units = addedUnits,
                        pricePerUnit = pricePerUnit,
                        type = com.prajwal.utilities.tools.wealthtracker.data.db.TransactionType.BUY
                    )
                )
            }
            
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

    fun syncPricesNow(onMessage: ((String) -> Unit)? = null) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val startTime = System.currentTimeMillis()
            try {
                // FIX #1: Use the already-subscribed `holdings` StateFlow directly.
                // The previous code called .stateIn(viewModelScope).value on a cold Flow,
                // which always returned an empty list before the first DB emission arrived.
                val currentHoldings = holdings.value
                val allHoldingsSynced = currentHoldings.isNotEmpty() && currentHoldings.all { it.latestPrice > 0.0 }
                var skippedCount = 0
                for (holding in currentHoldings) {
                    if (allHoldingsSynced && MarketDataRepository.shouldSkipSync(holding.lastUpdatedAt)) {
                        skippedCount++
                        continue
                    }

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
                
                if (currentHoldings.isNotEmpty() && skippedCount == currentHoldings.size) {
                    withContext(Dispatchers.Main) {
                        onMessage?.invoke("Market is closed. Prices are up to date.")
                    }
                }
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < 500) {
                    kotlinx.coroutines.delay(500 - elapsedTime)
                }
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
