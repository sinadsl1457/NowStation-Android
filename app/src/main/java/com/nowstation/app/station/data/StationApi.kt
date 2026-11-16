package com.nowstation.app.station.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface StationApi {
    @GET("api/stations")
    suspend fun search(@Query("query") query: String): List<StationDto>
}

@Serializable
data class StationDto(val name: String, val lines: List<String>)
