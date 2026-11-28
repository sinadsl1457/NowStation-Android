package com.nowstation.app.ride.ui

import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideStatus
import com.nowstation.app.ride.domain.RideNoticeKind
import com.nowstation.app.ride.domain.StripTrain

/** 화면과 잠금화면 알림이 같은 문구를 쓰도록 한곳에 둔다. */
object RideTexts {

    fun title(ride: ActiveRide): String {
        val v = ride.view
        return when {
            v.status == RideStatus.ARRIVED -> "${v.thisStation} 도착"
            // 서버가 내릴·갈아탈 역 바로 앞 역을 떠나면 켠다. 이름은 구간 끝(내릴·갈아탈 역)으로
            v.prepareToAlight && (v.target ?: v.thisStation) != null -> (v.target ?: v.thisStation).let { if (isTransfer(ride)) "곧 $it, 갈아탈 준비하세요" else "곧 $it, 내릴 준비하세요" }
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
            RideStatus.ARRIVED -> "도착"
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
            RideStatus.ARRIVED -> "도착했어요"
            RideStatus.UNKNOWN -> "상태를 확인하는 중"
        }
        return if (ride.online) main else "$main · 연결 끊김(다시 시도 중)"
    }

    /** 탑승 화면 위쪽 상태 표시. 연결이 끊기면 그게 먼저다(보이는 역이 오래된 것일 수 있음). */
    fun status(ride: ActiveRide): String {
        if (!ride.online) return "연결 끊김"
        return when (ride.view.status) {
            RideStatus.WAITING -> "기다리는 중"
            RideStatus.AT_STATION -> "정차 중"
            RideStatus.MOVING -> "달리는 중"
            RideStatus.HOLDING_BETWEEN -> "역 사이 정차"
            RideStatus.ARRIVED -> "도착"
            RideStatus.STALE, RideStatus.UNKNOWN -> "위치 확인 중"
        }
    }

    /**
     * "강남까지 8정거장": 늘 최종 목적지까지(사용자 피드백: '선릉까지 6정거장'은 선릉을 목적지로 오해하고, 환승이 여러 번이면 구간별로 다 못 보여 줌).
     * 환승은 그때그때 띠·진동으로만 알린다. 서버가 전체 수를 안 주면(옛 서버) 구간 끝까지. iOS와 같은 문구.
     */
    fun remaining(ride: ActiveRide): String? {
        val dest = ride.destination ?: ride.view.target ?: return null
        val n = ride.view.totalRemainingStops ?: ride.view.remainingStops ?: return null
        val name = if (ride.view.totalRemainingStops != null) dest else (ride.view.target ?: dest)
        return if (n == 0) "이번 역에서 내려요" else "${name}까지 ${n}정거장"
    }

    /** 노선도 위 열차의 그 역에서의 상태: 공공 데이터 그대로라 위쪽 "이번 정거장"(다가오는 역)과 헷갈리지 않게 적는다. */
    fun trainState(t: StripTrain): String? = when (t.status) {
        "ENTERING" -> "진입"
        "ARRIVED" -> "도착"
        "DEPARTED" -> "출발"
        else -> null
    }

    /** 타는 역까지 "약 N분", 타는 역에 있으면 "지금 타세요". 역간 평균 시간으로 센 추정이라 "약". iOS와 같은 문구. */
    fun eta(t: StripTrain): String = when (val m = t.etaMinutes) {
        null -> "지나감"
        0 -> "지금 타세요"
        else -> "약 ${m}분"
    }

    /** 노선도 위 열차 이름표: "왕십리행". */
    fun terminal(t: StripTrain): String = t.terminal?.let { "${it}행" } ?: ""

    /** 이번 구간 끝이 갈아탈 역인가(내릴 역이 아니라). */
    fun isTransfer(ride: ActiveRide): Boolean {
        val target = ride.view.target ?: return false
        val destination = ride.destination ?: return false
        return target != destination
    }

    /**
     * 파란 전체 화면을 띄울 때: 내릴 역에 다다랐거나 도착. 한 정거장 전(알림만 켜짐)에는 띄우지 않는다
     * (태평 → 야탑에서 모란에 들어서자마자 전체 화면이 떠 "이번 정거장 모란"을 못 봤다). 환승역은 띄우지 않는다.
     */
    fun showsAlightScreen(ride: ActiveRide): Boolean {
        val v = ride.view
        if (v.status == RideStatus.ARRIVED) return true
        return v.prepareToAlight && !isTransfer(ride) && v.thisStation == (v.target ?: v.thisStation)
    }

    /** 화면 위 안내 띠: 따라가는 열차, 한 정거장 전 내릴·갈아탈 준비, 환승역 도착. iOS와 같은 문구. */
    fun banner(ride: ActiveRide): String {
        val v = ride.view
        val target = v.target
        if (v.prepareToAlight && target != null && v.thisStation != target) {
            return if (isTransfer(ride)) "다음 역 $target · ${ride.view.legs.firstOrNull { it.from == target }?.line ?: "다음 노선"}으로 갈아타세요" else "다음 역 $target, 내릴 준비하세요"
        }
        val no = v.trainNo ?: return "탄 열차를 아래 노선도에서 골라 주세요"
        if (v.notice == RideNoticeKind.TRANSFER) {
            return "갈아타세요: ${v.line.orEmpty()} ${v.target.orEmpty()} 방면 $no 열차를 골라 뒀어요. 다른 열차면 아래에서 눌러 주세요"
        }
        return "$no 열차를 따라가고 있어요. 다른 열차면 아래에서 눌러 주세요"
    }
}
