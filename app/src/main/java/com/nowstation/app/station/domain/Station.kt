package com.nowstation.app.station.domain

import com.nowstation.app.core.result.AppResult

/** 역 이름과 지나는 노선(표시 이름: 2호선, 수인분당선). */
data class Station(val name: String, val lines: List<String>)

interface StationRepository {
    suspend fun search(query: String): AppResult<List<Station>>
    suspend fun lines(): AppResult<List<String>>
}
