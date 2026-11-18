package com.nowstation.app.station.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface StationApi {
    @GET("api/stations")
    suspend fun search(@Query("query") query: String): List<StationDto>

    /** 지원 노선의 표시 이름(2호선 등). */
    @GET("api/lines")
    suspend fun lines(): List<String>
}

@Serializable
data class StationDto(val name: String, val lines: List<String>)
