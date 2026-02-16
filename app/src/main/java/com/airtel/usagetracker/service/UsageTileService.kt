package com.airtel.usagetracker.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.airtel.usagetracker.data.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsageTileService : TileService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var usageRepository: UsageRepository

    override fun onCreate() {
        super.onCreate()
        usageRepository = UsageRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        
        val tile = qsTile
        tile.state = Tile.STATE_UNAVAILABLE
        tile.updateTile()

        serviceScope.launch {
            try {
                // Trigger refresh
                val result = usageRepository.fetchAndUpdateUsage()
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val usage = result.getOrNull()
                        val message = "Updated: ${String.format("%.1f", usage?.toGigabytes() ?: 0.0)} GB"
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Update Failed", Toast.LENGTH_SHORT).show()
                    }
                    updateTile()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateTile()
                }
            }
        }
    }
    
    private fun updateTile() {
        serviceScope.launch {
            try {
                val (cycleData, _) = usageRepository.getCurrentCycleUsage()
                val usageGb = cycleData.toGigabytes()
                
                val tile = qsTile
                if (tile != null) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "Airtel Usage"
                    tile.subtitle = "${String.format("%.1f", usageGb)} GB"
                    tile.updateTile()
                }
            } catch (e: Exception) {
                // Fallback if failing to read
                val tile = qsTile
                if (tile != null) {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "Airtel Usage"
                    tile.subtitle = "Tap to refresh"
                    tile.updateTile()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
