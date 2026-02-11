package com.airtel.usagetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airtel.usagetracker.data.UsageRepository
import com.airtel.usagetracker.data.models.RouterConfig
import com.airtel.usagetracker.data.models.UsageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    
    init {
        refreshUsageData()
    }
    
    fun refreshUsageData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.fetchAndUpdateUsage()
            
            if (result.isSuccess) {
                _usageData.value = result.getOrNull() ?: repository.getUsageData()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error"
                _usageData.value = repository.getUsageData()
            }
            
            _isLoading.value = false
        }
    }
    
    fun updateRouterConfig(config: RouterConfig) {
        repository.saveRouterConfig(config)
        _routerConfig.value = config
    }
}
