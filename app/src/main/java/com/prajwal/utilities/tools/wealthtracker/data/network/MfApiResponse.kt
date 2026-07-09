package com.prajwal.utilities.tools.wealthtracker.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MfApiResponse(
    @Json(name = "data") val data: List<MfNavData>? = null
)

@JsonClass(generateAdapter = true)
data class MfNavData(
    @Json(name = "nav") val nav: String? = null
)
