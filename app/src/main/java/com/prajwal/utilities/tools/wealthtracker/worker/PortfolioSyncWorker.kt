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
            val marketDataRepo = MarketDataRepository()

            val holdings = holdingsDao.getAllHoldings().first()

            for (holding in holdings) {
                val prices = if (holding.instrumentType == "MF") {
                    marketDataRepo.fetchMfNav(holding.identifier)
                } else {
                    marketDataRepo.fetchStockPrice(holding.identifier, holding.exchange)
                }

                if (prices != null) {
                    holdingsDao.updateHolding(
                        holding.copy(
                            latestPrice = prices.latestPrice,
                            previousClosePrice = prices.previousClosePrice,
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
