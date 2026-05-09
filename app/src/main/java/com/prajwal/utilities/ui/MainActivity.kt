package com.prajwal.utilities.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prajwal.utilities.core.navigation.AppNavHost
import com.prajwal.utilities.core.theme.UtilitiesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UtilitiesTheme {
                AppNavHost()
            }
        }
    }
}
