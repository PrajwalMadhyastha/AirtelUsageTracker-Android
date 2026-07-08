package com.prajwal.utilities.tools.wealthtracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prajwal.utilities.tools.wealthtracker.data.WealthPreferences
import com.prajwal.utilities.tools.wealthtracker.data.WealthRepository

class WealthTrackerViewModelFactory(
    private val repository: WealthRepository,
    private val prefs: WealthPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WealthTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WealthTrackerViewModel(repository, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
