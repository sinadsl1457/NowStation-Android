package com.nowstation.app.ride.domain

import com.nowstation.app.core.result.AppResult
import java.time.Instant

/** 서버가 알려 준 지금 상태. 열차 안 방송처럼 출발한 뒤에는 [thisStation]이 다가오는 역이다. */
data class RideView(
    val rideId: String,
    val status: RideStatus,
    val line: String?,
    val thisStation: String?,
    val nextStation: String?,
    val prepareToAlight: Boolean,
    /** 경로 구간들(태평 ▶ 선릉, 선릉 ▶ 강남). */
    val legs: List<RideLeg> = emptyList(),
    /** 따라가는(골라 둔) 열차 번호. */
    val trainNo: String? = null,
    /** false면 골라 두기만 한 열차(타는 역을 떠날 때 폰과 맞춰 확정). */
    val confirmed: Boolean = false,
    /** 이번 구간 끝(환승역·내릴 역)과 남은 정거장. */
    val target: String? = null,
    val remainingStops: Int? = null,
    val notice: RideNoticeKind? = null,
)

data class RideLeg(val line: String, val from: String, val to: String)

/** MISSED: 골라 둔 열차를 놓쳐 다음 열차로 바꿈, SWITCHED: 폰과 같이 출발한 다른 열차로 바꿈. */
enum class RideNoticeKind { MISSED, SWITCHED }

enum class RideStatus {
    /** 역에서 열차를 기다림(처음, 열차 놓침, 환승 중) */
    WAITING,
    AT_STATION,
    MOVING,
    /** 역 사이(터널)에서 멈춤 */
    HOLDING_BETWEEN,
    /** 탄 열차 소식이 2분 넘게 없음 */
    STALE,
    UNKNOWN,
}

/** 가로 노선도: 지금 구간 방향의 역(타는 역 몇 정거장 전부터 구간 끝까지)과 그 위 열차. 이번정거장과 같은 배치. */
data class TrainStrip(val leg: RideLeg, val stations: List<String>, val boardIndex: Int, val trains: List<StripTrain>)

/** [status]: ENTERING·ARRIVED·DEPARTED 등. [selected]: 지금 따라가는(골라 둔) 열차. */
data class StripTrain(
    val trainNo: String,
    val station: String,
    val status: String,
    val terminal: String?,
    val selected: Boolean,
    val seenSecondsAgo: Long,
)

/** 폰이 느낀 탈것의 움직임 변화(4일차: 가속도 센서). */
enum class MotionKind { MOVING, STOPPED }

data class Motion(val kind: MotionKind, val at: Instant)

interface RideRepository {
    suspend fun start(origin: String, destination: String?): AppResult<RideView>
    suspend fun get(rideId: String): AppResult<RideView>
    suspend fun sendMotion(rideId: String, motions: List<Motion>, sentAt: Instant): AppResult<RideView>
    /** 경로가 없으면(내릴 역 없음) null. */
    suspend fun strip(rideId: String): AppResult<TrainStrip?>
    suspend fun choose(rideId: String, line: String, trainNo: String): AppResult<RideView>
    suspend fun end(rideId: String): AppResult<Unit>
}
