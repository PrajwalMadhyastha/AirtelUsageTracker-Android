package com.prajwal.utilities.tools.wifiusage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prajwal.utilities.tools.wifiusage.data.ReportsRepository

class ReportsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(ReportsRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
