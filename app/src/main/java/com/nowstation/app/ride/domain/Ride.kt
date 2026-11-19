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
)

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

/** 지금 탄 열차로 고를 수 있는 열차 하나(서버가 출발역에 가까운 순으로 준다). [status]: ENTERING·ARRIVED·DEPARTED 등. */
data class TrainChoice(
    val line: String,
    val trainNo: String,
    val station: String,
    val status: String,
    val terminal: String?,
    val seenSecondsAgo: Long,
)

/** 폰이 느낀 탈것의 움직임 변화(4일차: 가속도 센서). */
enum class MotionKind { MOVING, STOPPED }

data class Motion(val kind: MotionKind, val at: Instant)

interface RideRepository {
    suspend fun start(origin: String, destination: String?): AppResult<RideView>
    suspend fun get(rideId: String): AppResult<RideView>
    suspend fun sendMotion(rideId: String, motions: List<Motion>, sentAt: Instant): AppResult<RideView>
    suspend fun trains(rideId: String): AppResult<List<TrainChoice>>
    suspend fun choose(rideId: String, line: String, trainNo: String): AppResult<RideView>
    suspend fun end(rideId: String): AppResult<Unit>
}
