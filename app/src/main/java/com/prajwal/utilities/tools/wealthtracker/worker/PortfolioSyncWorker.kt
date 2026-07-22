package com.prajwal.utilities.tools.wealthtracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prajwal.utilities.tools.wealthtracker.data.db.WealthDatabase
import com.prajwal.utilities.tools.wealthtracker.data.network.MarketDataRepository
import kotlinx.coroutines.flow.first

class PortfolioSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = WealthDatabase.getDatabase(applicationContext)
            val holdingsDao = db.holdingsDao()
            // FIX #2: Use the singleton — shares OkHttpClient with the ViewModel,
            // instead of creating a new thread pool + connection pool on every sync job.
            val marketDataRepo = MarketDataRepository.getInstance()

            val holdings = holdingsDao.getAllHoldings().first()

            for (holding in holdings) {
                if (holding.latestPrice > 0.0 && MarketDataRepository.shouldSkipSync(holding.lastUpdatedAt)) {
                    continue
                }

                val prices = if (holding.instrumentType == "MF") {
                    marketDataRepo.fetchMfNav(holding.identifier)
                } else {
                    marketDataRepo.fetchStockPrice(holding.identifier, holding.exchange)
                }

                if (prices != null) {
                    holdingsDao.updatePrice(
                        id = holding.id,
                        price = prices.latestPrice,
                        previousClosePrice = prices.previousClosePrice
                    )
                }
            }


            val prefs = com.prajwal.utilities.tools.wealthtracker.data.WealthPreferences(applicationContext)
            val autoSnapshotEnabled = prefs.autoSnapshotEnabled.first()
            if (autoSnapshotEnabled) {
                val autoSnapshotDay = prefs.autoSnapshotDayOfMonth.first()
                val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))
                if (now.dayOfMonth == autoSnapshotDay) {
                    val wealthDao = db.wealthDao()
                    val latestSnapshot = wealthDao.getLatestSnapshot()
                    
                    val todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
                    if (latestSnapshot == null || latestSnapshot.recordedAt < todayStart) {
                        val updatedHoldings = holdingsDao.getAllHoldings().first()
                        
                        var equityInv = 0.0
                        var goldInv = 0.0
                        var debtInv = 0.0
                        var silverInv = 0.0
                        var reitsInv = 0.0
                        
                        var equityCur = 0.0
                        var goldCur = 0.0
                        var debtCur = 0.0
                        var silverCur = 0.0
                        var reitsCur = 0.0

                        for (h in updatedHoldings) {
                            val cur = h.unitsHeld * h.latestPrice
                            val inv = h.investedAmount
                            when (h.assetClass) {
                                "Equity" -> { equityInv += inv; equityCur += cur }
                                "Gold" -> { goldInv += inv; goldCur += cur }
                                "Debt" -> { debtInv += inv; debtCur += cur }
                                "Silver" -> { silverInv += inv; silverCur += cur }
                                "REITs" -> { reitsInv += inv; reitsCur += cur }
                            }
                        }
                        
                        val newSnapshot = com.prajwal.utilities.tools.wealthtracker.data.db.AssetSnapshotEntity(
                            recordedAt = System.currentTimeMillis(),
                            equityInvested = equityInv,
                            goldInvested = goldInv,
                            debtInvested = debtInv,
                            silverInvested = silverInv,
                            reitsInvested = reitsInv,
                            equityCurrent = equityCur,
                            goldCurrent = goldCur,
                            debtCurrent = debtCur,
                            silverCurrent = silverCur,
                            reitsCurrent = reitsCur
                        )
                        wealthDao.insertSnapshot(newSnapshot)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
