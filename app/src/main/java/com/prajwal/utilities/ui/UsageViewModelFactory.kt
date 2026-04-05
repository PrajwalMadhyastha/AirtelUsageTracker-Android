package com.prajwal.utilities.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prajwal.utilities.data.UsageRepository

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
