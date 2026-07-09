package com.prajwal.utilities.tools.wealthtracker.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MfSearchResponseItem(
    @Json(name = "schemeCode") val schemeCode: Int,
    @Json(name = "schemeName") val schemeName: String
)
