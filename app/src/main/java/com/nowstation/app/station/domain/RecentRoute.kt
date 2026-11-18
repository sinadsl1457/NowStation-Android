package com.nowstation.app.station.domain

/** 탑승을 시작했던 경로. 홈에서 눌러 바로 다시 시작한다. */
data class RecentRoute(val origin: Station, val destination: Station?) {
    companion object {
        const val MAX = 3

        /** 맨 앞에 넣는다. 같은 경로(역 이름 기준)는 하나만 남기고, [MAX]개까지. */
        fun push(routes: List<RecentRoute>, route: RecentRoute): List<RecentRoute> =
            (listOf(route) + routes.filterNot { it.sameAs(route) }).take(MAX)
    }

    private fun sameAs(other: RecentRoute) = origin.name == other.origin.name && destination?.name == other.destination?.name
}

/** 폰 안에만 둔다(서버로 보내지 않음). */
interface RecentRouteStore {
    fun load(): List<RecentRoute>
    fun save(routes: List<RecentRoute>)
}
