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

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
