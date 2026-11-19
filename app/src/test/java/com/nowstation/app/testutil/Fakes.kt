package com.nowstation.app.testutil

import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.Motion
import com.nowstation.app.ride.domain.RideRepository
import com.nowstation.app.ride.domain.RideStatus
import com.nowstation.app.ride.domain.RideView
import com.nowstation.app.ride.domain.TrainStrip
import com.nowstation.app.ride.service.RideServiceLauncher
import com.nowstation.app.station.domain.RecentRoute
import com.nowstation.app.station.domain.RecentRouteStore
import com.nowstation.app.station.domain.Station
import com.nowstation.app.station.domain.StationRepository
import java.time.Instant

fun view(status: RideStatus = RideStatus.WAITING, thisStation: String? = "태평", next: String? = null, line: String? = null, alight: Boolean = false) =
    RideView("r1", status, line, thisStation, next, alight)

class FakeRideRepository : RideRepository {
    var startResult: AppResult<RideView> = AppResult.Success(view())
    var getResult: AppResult<RideView> = AppResult.Success(view())
    /** null이면 getResult와 같다. */
    var motionResult: AppResult<RideView>? = null
    val sentMotions = mutableListOf<Pair<List<Motion>, Instant>>()
    val ended = mutableListOf<String>()

    var lastStart: Pair<String, String?>? = null

    override suspend fun start(origin: String, destination: String?): AppResult<RideView> {
        lastStart = origin to destination
        return startResult
    }
    override suspend fun get(rideId: String) = getResult
    override suspend fun sendMotion(rideId: String, motions: List<Motion>, sentAt: Instant): AppResult<RideView> {
        sentMotions += motions to sentAt
        return motionResult ?: getResult
    }
    var stripResult: AppResult<TrainStrip?> = AppResult.Success(null)
    var chooseResult: AppResult<RideView> = AppResult.Success(view())
    val chosen = mutableListOf<String>()
    var stripCalls = 0
    override suspend fun strip(rideId: String): AppResult<TrainStrip?> {
        stripCalls++
        return stripResult
    }
    override suspend fun choose(rideId: String, line: String, trainNo: String): AppResult<RideView> {
        chosen += trainNo
        return chooseResult
    }
    override suspend fun end(rideId: String): AppResult<Unit> {
        ended += rideId
        return AppResult.Success(Unit)
    }
}

class FakeStationRepository(var stations: List<Station> = emptyList()) : StationRepository {
    val queries = mutableListOf<String>()
    override suspend fun search(query: String): AppResult<List<Station>> {
        queries += query
        return AppResult.Success(stations.filter { query.isEmpty() || it.name.contains(query) })
    }
    var linesResult: AppResult<List<String>> = AppResult.Success(listOf("2호선", "수인분당선", "신분당선"))
    override suspend fun lines() = linesResult
}

class FakeLauncher : RideServiceLauncher {
    var started = 0
    override fun start() { started++ }
}

class FakeRecentRouteStore(var routes: List<RecentRoute> = emptyList()) : RecentRouteStore {
    override fun load() = routes
    override fun save(routes: List<RecentRoute>) { this.routes = routes }
}
