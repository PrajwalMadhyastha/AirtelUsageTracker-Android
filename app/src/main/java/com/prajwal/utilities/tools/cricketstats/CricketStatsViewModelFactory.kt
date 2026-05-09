package com.prajwal.utilities.tools.cricketstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prajwal.utilities.tools.cricketstats.data.CricketStatsRepository

class CricketStatsViewModelFactory(
    private val repository: CricketStatsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CricketStatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CricketStatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
