package com.prajwal.utilities.tools.wealthtracker.data.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

data class AssetSearchResult(
    val identifier: String,
    val name: String,
    val exchange: String?
)

data class AssetPrices(
    val latestPrice: Double,
    val previousClosePrice: Double
)

class MarketDataRepository {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/") // Dummy URL as we pass absolute URLs
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(MarketDataService::class.java)

    suspend fun fetchMfNav(schemeCode: String): AssetPrices? {
        if (schemeCode.startsWith("SGB", ignoreCase = true)) {
            return fetchSgbPriceFromMoneycontrol(schemeCode, null)
        }
        return try {
            val response = service.getMfNav("https://api.mfapi.in/mf/$schemeCode")
            val navs = response.data ?: emptyList()
            if (navs.isNotEmpty()) {
                val latest = navs[0].nav?.toDoubleOrNull() ?: 0.0
                val previous = if (navs.size > 1) navs[1].nav?.toDoubleOrNull() ?: latest else latest
                AssetPrices(latest, previous)
            } else null
        } catch (e: Exception) {
            Log.e("MarketDataRepo", "Error fetching MF NAV", e)
            null
        }
    }

    suspend fun fetchStockPrice(ticker: String, exchange: String?): AssetPrices? {
        if (ticker.startsWith("SGB", ignoreCase = true)) {
            return fetchSgbPriceFromMoneycontrol(ticker, exchange)
        }
        val suffix = when (exchange) {
            "NSE" -> ".NS"
            "BSE" -> ".BO"
            else -> ""
        }
        val symbol = "$ticker$suffix"
        
        return try {
            val response = service.getYahooChart("https://query2.finance.yahoo.com/v8/finance/chart/$symbol")
            val meta = response.chart?.result?.firstOrNull()?.meta
            if (meta?.regularMarketPrice != null) {
                AssetPrices(meta.regularMarketPrice, meta.chartPreviousClose ?: meta.regularMarketPrice)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchSgbPriceFromMoneycontrol(ticker: String, exchange: String?): AssetPrices? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cleanTicker = ticker.replace("-GB", "").replace(".NS", "").replace(".BO", "")
            val searchRequest = okhttp3.Request.Builder()
                .url("https://www.moneycontrol.com/mccode/common/autosuggestion_solr.php?query=$cleanTicker&type=1&format=json")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
                
            val searchResponse = okHttpClient.newCall(searchRequest).execute()
            if (!searchResponse.isSuccessful()) {
                Log.e("MarketDataRepo", "Search failed with code ${searchResponse.code()}")
                return@withContext null
            }
            
            val searchBody = searchResponse.body()?.string() ?: ""
            // Try to find the link_src in the JSON array
            val linkMatch = Regex(""""link_src"\s*:\s*"([^"]+)"""").find(searchBody)
            if (linkMatch != null) {
                val detailUrlStr = linkMatch.groupValues[1]
                
                val detailRequest = okhttp3.Request.Builder()
                    .url(detailUrlStr)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                    
                val detailResponse = okHttpClient.newCall(detailRequest).execute()
                if (!detailResponse.isSuccessful()) {
                    Log.e("MarketDataRepo", "Detail failed with code ${detailResponse.code()}")
                    return@withContext null
                }
                
                val detailBody = detailResponse.body()?.string() ?: ""
                
                // Parse nsespotval or bsespotval
                val nseMatch = Regex("""id="nsespotval"[^>]*value="([0-9,.]+)"""").find(detailBody)
                val bseMatch = Regex("""id="bsespotval"[^>]*value="([0-9,.]+)"""").find(detailBody)
                
                val priceStr = nseMatch?.groupValues?.get(1) ?: bseMatch?.groupValues?.get(1)
                
                val price = priceStr?.replace(",", "")?.toDoubleOrNull()
                if (price != null) {
                    return@withContext AssetPrices(price, price) // Mock previous close as current
                } else {
                    Log.e("MarketDataRepo", "Price not found in HTML. detailUrl: $detailUrlStr")
                }
            } else {
                Log.e("MarketDataRepo", "Link not found in JSON. searchBody: $searchBody")
            }
            null
        } catch (e: Exception) {
            Log.e("MarketDataRepo", "Error fetching SGB from Moneycontrol", e)
            null
        }
    }

    suspend fun searchAsset(query: String, isMf: Boolean): List<AssetSearchResult> {
        if (query.length < 2) return emptyList()
        return try {
            if (isMf) {
                val results = service.searchMfScheme(query = query)
                results.map {
                    AssetSearchResult(
                        identifier = it.schemeCode.toString(),
                        name = it.schemeName,
                        exchange = null
                    )
                }
            } else {
                val results = service.searchYahooTicker(query = query)
                results.quotes
                    .filter { it.quoteType == "EQUITY" || it.quoteType == "ETF" || it.quoteType == "MUTUALFUND" || it.quoteType == null }
                    .map { quote ->
                        val parts = quote.symbol.split(".")
                        val identifier = parts[0]
                        val exchange = if (parts.size > 1) {
                            when (parts[1]) {
                                "NS" -> "NSE"
                                "BO" -> "BSE"
                                else -> quote.exchDisp
                            }
                        } else {
                            quote.exchDisp
                        }
                        AssetSearchResult(
                            identifier = identifier,
                            name = quote.longname ?: quote.shortname ?: quote.symbol,
                            exchange = exchange
                        )
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
