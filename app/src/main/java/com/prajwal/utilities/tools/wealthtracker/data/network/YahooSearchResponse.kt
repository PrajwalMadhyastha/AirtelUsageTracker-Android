package com.prajwal.utilities.tools.wealthtracker.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YahooSearchResponse(
    @Json(name = "quotes") val quotes: List<YahooSearchQuote>
)

@JsonClass(generateAdapter = true)
data class YahooSearchQuote(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "shortname") val shortname: String? = null,
    @Json(name = "longname") val longname: String? = null,
    @Json(name = "exchDisp") val exchDisp: String? = null,
    @Json(name = "quoteType") val quoteType: String? = null
)
