package com.nowstation.app.ride

import com.nowstation.app.ride.domain.RideLeg
import com.nowstation.app.ride.domain.RideNoticeKind
import com.nowstation.app.ride.domain.StripTrain
import com.nowstation.app.ride.domain.TrainStrip
import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.data.RideApi
import com.nowstation.app.ride.data.RideRepositoryImpl
import com.nowstation.app.ride.domain.Motion
import com.nowstation.app.ride.domain.MotionKind
import com.nowstation.app.ride.domain.RideStatus
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant

/** 서버 응답 모양은 NowStation-Server의 RideController와 같다. */
class RideRepositoryImplTest {
    private val server = MockWebServer().apply { start() }
    private val api = Retrofit.Builder().baseUrl(server.url("/"))
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build().create(RideApi::class.java)
    private val repo = RideRepositoryImpl(api)

    @After fun tearDown() = server.shutdown()

    @Test
    fun `탑승 응답을 화면 상태로 바꾼다(모르는 칸은 무시)`() = runTest {
        server.enqueue(MockResponse().setBody("""{"rideId":"r1","status":"MOVING","line":"2호선","thisStation":"강남","nextStation":"교대","prepareToAlight":true,"extra":1}"""))

        val view = (repo.get("r1") as AppResult.Success).data

        assertEquals(RideStatus.MOVING, view.status)
        assertEquals("강남", view.thisStation)
        assertTrue(view.prepareToAlight)
        assertEquals("/api/rides/r1", server.takeRequest().path)
    }

    @Test
    fun `모르는 상태 값은 UNKNOWN`() = runTest {
        server.enqueue(MockResponse().setBody("""{"rideId":"r1","status":"NEW_STATE"}"""))

        assertEquals(RideStatus.UNKNOWN, (repo.get("r1") as AppResult.Success).data.status)
    }

    @Test
    fun `400은 서버가 준 이유(ProblemDetail detail), 404는 NotFound`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"status":400,"detail":"모르는 역: 없는역"}"""))
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"status":404}"""))

        assertEquals(AppResult.Error(AppError.BadRequest("모르는 역: 없는역")), repo.start("없는역", null))
        assertEquals(AppResult.Error(AppError.NotFound), repo.get("nope"))
    }

    @Test
    fun `서버에 닿지 못하면 Network`() = runTest {
        server.shutdown()

        assertEquals(AppResult.Error(AppError.Network), repo.get("r1"))
    }

    @Test
    fun `움직임은 종류와 시각을 ISO 문자열로 보낸다`() = runTest {
        server.enqueue(MockResponse().setBody("""{"rideId":"r1","status":"WAITING"}"""))

        repo.sendMotion("r1", listOf(Motion(MotionKind.MOVING, Instant.parse("2026-11-17T00:00:05Z"))), Instant.parse("2026-11-17T00:00:10Z"))

        val body = server.takeRequest().body.readUtf8()
        assertEquals("""{"events":[{"kind":"MOVING","at":"2026-11-17T00:00:05Z"}],"sentAt":"2026-11-17T00:00:10Z"}""", body)
    }

    @Test
    fun `노선도와 고르기는 서버 경로·모양 그대로, 경로가 없으면(204) null`() = runTest {
        server.enqueue(MockResponse().setBody("""{"leg":{"line":"수인분당선","from":"태평","to":"선릉"},"stations":["모란","태평","가천대"],"boardIndex":1,"trains":[{"trainNo":"6206","station":"모란","status":"ARRIVED","terminal":"왕십리","selected":true,"seenSecondsAgo":12}]}"""))
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setBody("""{"rideId":"r1","status":"AT_STATION","line":"수인분당선","thisStation":"모란","legs":[{"line":"수인분당선","from":"태평","to":"선릉"}],"trainNo":"6208","target":"선릉","remainingStops":10,"notice":"TRANSFER"}"""))

        val strip = (repo.strip("r1") as AppResult.Success).data!!
        assertEquals(TrainStrip(RideLeg("수인분당선", "태평", "선릉"), listOf("모란", "태평", "가천대"), 1, listOf(StripTrain("6206", "모란", "ARRIVED", "왕십리", true, 12))), strip)
        assertEquals("/api/rides/r1/trains", server.takeRequest().path)
        assertEquals(AppResult.Success(null), repo.strip("r1"))
        server.takeRequest()

        val view = (repo.choose("r1", "수인분당선", "6208") as AppResult.Success).data
        assertEquals("6208", view.trainNo)
        assertEquals(10, view.remainingStops)
        assertEquals(RideNoticeKind.TRANSFER, view.notice)
        val request = server.takeRequest()
        assertEquals("/api/rides/r1/train", request.path)
        assertEquals("""{"line":"수인분당선","trainNo":"6208"}""", request.body.readUtf8())
    }
}
