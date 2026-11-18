package com.nowstation.app.station.data

import com.nowstation.app.core.result.AppResult
import com.nowstation.app.core.result.safeApiCall
import com.nowstation.app.station.domain.Station
import com.nowstation.app.station.domain.StationRepository
import javax.inject.Inject

class StationRepositoryImpl @Inject constructor(private val api: StationApi) : StationRepository {
    override suspend fun search(query: String): AppResult<List<Station>> =
        safeApiCall { api.search(query).map { Station(it.name, it.lines) } }

    override suspend fun lines(): AppResult<List<String>> = safeApiCall { api.lines() }
}
