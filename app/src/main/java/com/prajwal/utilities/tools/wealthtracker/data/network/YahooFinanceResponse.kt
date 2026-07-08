package com.prajwal.utilities.tools.wealthtracker.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YahooFinanceResponse(
    @Json(name = "chart") val chart: YahooChart? = null
)

@JsonClass(generateAdapter = true)
data class YahooChart(
    @Json(name = "result") val result: List<YahooResult>? = null
)

@JsonClass(generateAdapter = true)
data class YahooResult(
    @Json(name = "meta") val meta: YahooMeta? = null
)

@JsonClass(generateAdapter = true)
data class YahooMeta(
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double? = null,
    @Json(name = "chartPreviousClose") val chartPreviousClose: Double? = null
)
