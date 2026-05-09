package com.prajwal.utilities.tools.crickettoss

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prajwal.utilities.tools.crickettoss.data.CricketTossRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CricketTossViewModel(private val repository: CricketTossRepository) : ViewModel() {
    
    val history = repository.recentTosses
        .map { list -> list.map { it.result } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveToss(result: String) {
        viewModelScope.launch {
            repository.saveToss(result)
        }
    }
}

class CricketTossViewModelFactory(private val repository: CricketTossRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CricketTossViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CricketTossViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
