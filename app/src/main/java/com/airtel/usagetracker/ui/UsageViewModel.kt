package com.airtel.usagetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airtel.usagetracker.data.UsageRepository
import com.airtel.usagetracker.data.models.RouterConfig
import com.airtel.usagetracker.data.models.UsageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UsageViewModel(private val repository: UsageRepository) : ViewModel() {
    
    private val _usageData = MutableStateFlow(repository.getUsageData())
    val usageData: StateFlow<UsageData> = _usageData.asStateFlow()
    
    private val _routerConfig = MutableStateFlow(repository.getRouterConfig())
    val routerConfig: StateFlow<RouterConfig> = _routerConfig.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _debugInfo = MutableStateFlow(repository.getDebugInfo())
    val debugInfo: StateFlow<com.airtel.usagetracker.data.models.DebugInfo> = _debugInfo.asStateFlow()

    private val _scrapingStatus = MutableStateFlow(com.airtel.usagetracker.data.models.ScrapingStatus.IDLE)
    val scrapingStatus: StateFlow<com.airtel.usagetracker.data.models.ScrapingStatus> = _scrapingStatus.asStateFlow()


    
    // Preferences
    val isOnboardingCompleted: StateFlow<Boolean> = repository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val syncIntervalHours: StateFlow<Int> = repository.syncIntervalHours
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val isAutoSyncEnabled: StateFlow<Boolean> = repository.isAutoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Only refresh if onboarding is completed
        viewModelScope.launch {
            isOnboardingCompleted.collect { completed ->
                if (completed) {
                    refreshUsageData()
                }
            }
        }
    }
    
    fun refreshUsageData() {
        viewModelScope.launch {
            if (!repository.isWifiConnected()) {
                _errorMessage.value = "Not connected to WiFi"
                _scrapingStatus.value = com.airtel.usagetracker.data.models.ScrapingStatus.ERROR
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null
            _scrapingStatus.value = com.airtel.usagetracker.data.models.ScrapingStatus.CONNECTING
            
            // Collect status updates from repository
            val statusJob = launch {
                repository.scrapingStatus.collect { status ->
                    _scrapingStatus.value = status
                }
            }
            
            val result = repository.fetchAndUpdateUsage()
            
            if (result.isSuccess) {
                _usageData.value = result.getOrNull() ?: repository.getUsageData()
                _scrapingStatus.value = com.airtel.usagetracker.data.models.ScrapingStatus.SUCCESS
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error"
                _usageData.value = repository.getUsageData()
                _scrapingStatus.value = com.airtel.usagetracker.data.models.ScrapingStatus.ERROR
            }
            
            // Always update debug info after fetch attempt
            _debugInfo.value = repository.getDebugInfo()
            
            statusJob.cancel()
            _isLoading.value = false
        }
    }
    
    fun updateRouterConfig(config: RouterConfig) {
        repository.saveRouterConfig(config)
        _routerConfig.value = config
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            // Initial sync on onboarding complete
            refreshUsageData()
        }
    }

    fun setSyncInterval(hours: Int) {
        viewModelScope.launch {
            repository.setSyncInterval(hours)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
        }
    }
}
