package com.nowstation.app.ride

import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.Motion
import com.nowstation.app.ride.domain.MotionKind
import com.nowstation.app.ride.domain.RideNotice
import com.nowstation.app.ride.domain.RideStatus
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.testutil.FakeRideRepository
import com.nowstation.app.testutil.view
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RideTrackerTest {
    private val repo = FakeRideRepository()
    private val clock = Clock.fixed(Instant.parse("2026-11-17T00:00:00Z"), ZoneOffset.UTC)
    private val tracker = RideTracker(repo, clock)

    @Test
    fun `시작에 성공하면 진행 중인 탑승이 생긴다`() = runTest {
        tracker.start("태평", "강남")

        val ride = tracker.state.value!!
        assertEquals("태평", ride.origin)
        assertEquals("강남", ride.destination)
        assertTrue(ride.online)
    }

    @Test
    fun `시작에 실패하면 탑승은 생기지 않는다`() = runTest {
        repo.startResult = AppResult.Error(AppError.BadRequest("모르는 역: 없는역"))

        val result = tracker.start("없는역", null)

        assertTrue(result is AppResult.Error)
        assertNull(tracker.state.value)
    }

    @Test
    fun `새로 받아 오면 화면 상태를 바꾼다`() = runTest {
        tracker.start("태평", null)
        repo.getResult = AppResult.Success(view(RideStatus.MOVING, "가천대", "복정", "수인분당선"))

        tracker.refresh()

        assertEquals("가천대", tracker.state.value!!.view.thisStation)
    }

    @Test
    fun `연결이 끊기면 마지막 역을 그대로 두고 끊김만 표시한다`() = runTest {
        tracker.start("태평", null)
        repo.getResult = AppResult.Success(view(RideStatus.MOVING, "가천대", "복정"))
        tracker.refresh()
        repo.getResult = AppResult.Error(AppError.Network)

        tracker.refresh()

        val ride = tracker.state.value!!
        assertEquals("가천대", ride.view.thisStation)
        assertFalse(ride.online)
    }

    @Test
    fun `서버에서 탑승이 사라지면 앱도 끝내고 알린다`() = runTest {
        tracker.start("태평", null)
        repo.getResult = AppResult.Error(AppError.NotFound)

        tracker.refresh()

        assertNull(tracker.state.value)
        assertEquals(RideNotice.Lost, tracker.notices.first())
    }

    @Test
    fun `끝내면 서버에도 알린다`() = runTest {
        tracker.start("태평", null)

        tracker.end()

        assertNull(tracker.state.value)
        assertEquals(listOf("r1"), repo.ended)
    }

    @Test
    fun `움직임은 보낸 시각과 함께 보낸다`() = runTest {
        tracker.start("태평", null)
        val moving = Motion(MotionKind.MOVING, Instant.parse("2026-11-16T23:59:50Z"))

        tracker.sendMotion(listOf(moving))

        assertEquals(listOf(moving) to clock.instant(), repo.sentMotions.single())
    }
}
