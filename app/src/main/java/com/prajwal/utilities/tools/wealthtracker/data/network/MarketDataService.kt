package com.prajwal.utilities.tools.wealthtracker.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface MarketDataService {
    @GET
    suspend fun getMfNav(@Url url: String): MfApiResponse

    @GET
    suspend fun getYahooChart(@Url url: String): YahooFinanceResponse

    @GET
    suspend fun searchMfScheme(
        @Url url: String = "https://api.mfapi.in/mf/search",
        @Query("q") query: String
    ): List<MfSearchResponseItem>

    @GET
    suspend fun searchYahooTicker(
        @Url url: String = "https://query2.finance.yahoo.com/v1/finance/search",
        @Query("q") query: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0"
    ): YahooSearchResponse
}
