package com.nowstation.app.ride.ui

import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideStatus

/** 화면과 잠금화면 알림이 같은 문구를 쓰도록 한곳에 둔다. */
object RideTexts {

    fun title(ride: ActiveRide): String {
        val v = ride.view
        return when {
            v.prepareToAlight && v.thisStation != null -> "곧 ${v.thisStation}, 내릴 준비하세요"
            v.status == RideStatus.WAITING -> "${v.thisStation ?: ride.origin}역에서 기다리는 중"
            v.status == RideStatus.STALE || v.thisStation == null -> "위치 확인 중"
            else -> "이번 역 ${v.thisStation}"
        }
    }

    /**
     * 상태 표시줄 칩(Android 16 Live Updates)에 들어갈 짧은 글자. 칩은 몇 글자만 보여서 이번 역 이름만 둔다.
     * 열차 소식이 없을 때는 틀린 역을 붙여 두지 않도록 비운다.
     */
    fun chip(ride: ActiveRide): String? {
        val v = ride.view
        return when (v.status) {
            RideStatus.WAITING -> "대기"
            RideStatus.AT_STATION, RideStatus.MOVING, RideStatus.HOLDING_BETWEEN -> v.thisStation
            RideStatus.STALE, RideStatus.UNKNOWN -> null
        }
    }

    fun detail(ride: ActiveRide): String {
        val v = ride.view
        val main = when (v.status) {
            RideStatus.WAITING -> "열차에 타면 자동으로 따라가요"
            RideStatus.AT_STATION -> listOfNotNull("정차 중", v.nextStation?.let { "다음 역 $it" }).joinToString(" · ")
            RideStatus.MOVING -> v.nextStation?.let { "다음 역 $it" } ?: "종점 방면"
            RideStatus.HOLDING_BETWEEN -> "역 사이에서 잠시 정차 중"
            RideStatus.STALE -> v.thisStation?.let { "마지막으로 확인한 역 $it" } ?: "열차 소식을 기다리는 중"
            RideStatus.UNKNOWN -> "상태를 확인하는 중"
        }
        return if (ride.online) main else "$main · 연결 끊김(다시 시도 중)"
    }
}
