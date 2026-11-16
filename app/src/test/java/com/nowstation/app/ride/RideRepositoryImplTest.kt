package com.nowstation.app.ride

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
}
