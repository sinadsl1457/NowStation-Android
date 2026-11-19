package com.nowstation.app.ride.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** NowStation-Server의 탑승 API(README의 API 표와 같은 모양). */
interface RideApi {
    @POST("api/rides")
    suspend fun start(@Body request: StartRideRequest): RideDto

    @POST("api/rides/{id}/motion")
    suspend fun motion(@Path("id") id: String, @Body request: MotionRequest): RideDto

    /** 경로가 없으면 204. */
    @GET("api/rides/{id}/trains")
    suspend fun strip(@Path("id") id: String): Response<TrainStripDto>

    @POST("api/rides/{id}/train")
    suspend fun choose(@Path("id") id: String, @Body request: ChooseTrainRequest): RideDto

    @GET("api/rides/{id}")
    suspend fun get(@Path("id") id: String): RideDto

    @DELETE("api/rides/{id}")
    suspend fun end(@Path("id") id: String)
}

@Serializable
data class StartRideRequest(val originStation: String, val destinationStation: String? = null)

@Serializable
data class MotionRequest(val events: List<MotionItem>, val sentAt: String)

@Serializable
data class MotionItem(val kind: String, val at: String)

@Serializable
data class LegDto(val line: String, val from: String, val to: String, val stations: List<String> = emptyList())

@Serializable
data class TrainStripDto(val leg: LegDto, val stations: List<String>, val boardIndex: Int, val trains: List<StripTrainDto>)

@Serializable
data class StripTrainDto(
    val trainNo: String,
    val station: String,
    val status: String,
    val terminal: String? = null,
    val selected: Boolean = false,
    val seenSecondsAgo: Long = 0,
)

@Serializable
data class ChooseTrainRequest(val line: String, val trainNo: String)

@Serializable
data class RideDto(
    val rideId: String,
    val status: String,
    val line: String? = null,
    val thisStation: String? = null,
    val nextStation: String? = null,
    val prepareToAlight: Boolean = false,
    val legs: List<LegDto> = emptyList(),
    val trainNo: String? = null,
    val target: String? = null,
    val remainingStops: Int? = null,
    val notice: String? = null,
)
