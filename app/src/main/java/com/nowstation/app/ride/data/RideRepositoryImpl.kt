package com.nowstation.app.ride.data

import com.nowstation.app.core.result.AppResult
import com.nowstation.app.core.result.safeApiCall
import com.nowstation.app.ride.domain.Motion
import com.nowstation.app.ride.domain.RideRepository
import com.nowstation.app.ride.domain.RideStatus
import com.nowstation.app.ride.domain.RideView
import com.nowstation.app.ride.domain.TrainChoice
import java.time.Instant
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(private val api: RideApi) : RideRepository {
    override suspend fun start(origin: String, destination: String?): AppResult<RideView> =
        safeApiCall { api.start(StartRideRequest(origin, destination)).toDomain() }

    override suspend fun get(rideId: String): AppResult<RideView> = safeApiCall { api.get(rideId).toDomain() }

    override suspend fun sendMotion(rideId: String, motions: List<Motion>, sentAt: Instant): AppResult<RideView> =
        safeApiCall {
            api.motion(rideId, MotionRequest(motions.map { MotionItem(it.kind.name, it.at.toString()) }, sentAt.toString())).toDomain()
        }

    override suspend fun trains(rideId: String): AppResult<List<TrainChoice>> = safeApiCall {
        api.trains(rideId).map { TrainChoice(it.line, it.trainNo, it.station, it.status, it.terminal, it.seenSecondsAgo) }
    }

    override suspend fun choose(rideId: String, line: String, trainNo: String): AppResult<RideView> =
        safeApiCall { api.choose(rideId, ChooseTrainRequest(line, trainNo)).toDomain() }

    override suspend fun end(rideId: String): AppResult<Unit> = safeApiCall { api.end(rideId) }
}

/** 모르는 상태 값이 와도(서버가 먼저 바뀐 경우) 앱이 죽지 않게 UNKNOWN. */
internal fun RideDto.toDomain() = RideView(
    rideId = rideId,
    status = RideStatus.entries.firstOrNull { it.name == status } ?: RideStatus.UNKNOWN,
    line = line,
    thisStation = thisStation,
    nextStation = nextStation,
    prepareToAlight = prepareToAlight,
)
