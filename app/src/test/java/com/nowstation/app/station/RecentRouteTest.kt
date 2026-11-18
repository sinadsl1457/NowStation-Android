package com.nowstation.app.station

import com.nowstation.app.station.domain.RecentRoute
import com.nowstation.app.station.domain.Station
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentRouteTest {
    private val taepyeong = Station("태평", listOf("수인분당선"))
    private val seohyeon = Station("서현", listOf("수인분당선"))
    private val gangnam = Station("강남", listOf("2호선", "신분당선"))
    private val pangyo = Station("판교", listOf("신분당선"))

    private fun names(routes: List<RecentRoute>) = routes.map { "${it.origin.name}>${it.destination?.name}" }

    @Test
    fun `새 경로는 맨 앞, 같은 경로는 하나만`() {
        var routes = RecentRoute.push(emptyList(), RecentRoute(taepyeong, seohyeon))
        routes = RecentRoute.push(routes, RecentRoute(pangyo, gangnam))
        routes = RecentRoute.push(routes, RecentRoute(taepyeong, seohyeon))

        assertEquals(listOf("태평>서현", "판교>강남"), names(routes))
    }

    @Test
    fun `내릴 역이 다르면 다른 경로, 최대 3개`() {
        var routes = emptyList<RecentRoute>()
        for (r in listOf(RecentRoute(taepyeong, null), RecentRoute(taepyeong, seohyeon), RecentRoute(pangyo, gangnam), RecentRoute(gangnam, null))) {
            routes = RecentRoute.push(routes, r)
        }

        assertEquals(listOf("강남>null", "판교>강남", "태평>서현"), names(routes))
    }
}
