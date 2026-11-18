package com.nowstation.app.station.data

import android.content.SharedPreferences
import com.nowstation.app.station.domain.RecentRoute
import com.nowstation.app.station.domain.RecentRouteStore
import com.nowstation.app.station.domain.Station
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** 목록 전체를 한 키에 JSON으로(반쪽 저장 없음). 읽지 못하면 빈 목록: 최근 경로는 없어도 앱이 돌아간다. */
class PrefsRecentRouteStore @Inject constructor(private val prefs: SharedPreferences) : RecentRouteStore {

    override fun load(): List<RecentRoute> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<RouteJson>>(raw).map { it.toDomain() } }.getOrDefault(emptyList())
    }

    override fun save(routes: List<RecentRoute>) {
        prefs.edit().putString(KEY, json.encodeToString(routes.map { RouteJson.of(it) })).apply()
    }

    @Serializable
    private data class StationJson(val name: String, val lines: List<String>)

    @Serializable
    private data class RouteJson(val origin: StationJson, val destination: StationJson? = null) {
        fun toDomain() = RecentRoute(Station(origin.name, origin.lines), destination?.let { Station(it.name, it.lines) })

        companion object {
            fun of(r: RecentRoute) = RouteJson(
                StationJson(r.origin.name, r.origin.lines),
                r.destination?.let { StationJson(it.name, it.lines) },
            )
        }
    }

    private companion object {
        const val KEY = "recent_routes_v1"
        val json = Json { ignoreUnknownKeys = true }
    }
}
