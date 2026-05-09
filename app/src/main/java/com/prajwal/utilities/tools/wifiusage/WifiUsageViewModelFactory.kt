package com.prajwal.utilities.tools.wifiusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prajwal.utilities.tools.wifiusage.data.UsageRepository

class UsageViewModelFactory(
    private val repository: UsageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
