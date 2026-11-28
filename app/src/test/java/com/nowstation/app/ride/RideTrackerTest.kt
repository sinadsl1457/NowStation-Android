package com.nowstation.app.ride

import com.nowstation.app.ride.domain.RideLeg
import com.nowstation.app.ride.domain.StripTrain
import com.nowstation.app.ride.domain.TrainStrip
import com.nowstation.app.core.result.AppError
import com.nowstation.app.core.result.AppResult
import com.nowstation.app.ride.domain.ActiveRide
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
    fun `움직임은 보낸 시각과 함께 바로 보낸다`() = runTest {
        tracker.start("태평", null)
        val moving = Motion(MotionKind.MOVING, Instant.parse("2026-11-16T23:59:50Z"))

        tracker.reportMotion(moving)

        assertEquals(listOf(moving) to clock.instant(), repo.sentMotions.single())
        assertEquals(0, tracker.state.value!!.unsentMotions)
    }

    @Test
    fun `지하에서 못 보낸 움직임은 쌓았다가 다음 갱신 때 한꺼번에 보낸다`() = runTest {
        tracker.start("태평", null)
        val moving = Motion(MotionKind.MOVING, Instant.parse("2026-11-16T23:58:00Z"))
        val stopped = Motion(MotionKind.STOPPED, Instant.parse("2026-11-16T23:59:30Z"))
        repo.motionResult = AppResult.Error(AppError.Network)
        tracker.reportMotion(moving)
        tracker.reportMotion(stopped)
        assertEquals(2, tracker.state.value!!.unsentMotions)

        repo.motionResult = null
        repo.getResult = AppResult.Success(view(RideStatus.AT_STATION, "가천대", "복정"))
        tracker.refresh()

        assertEquals(listOf(moving, stopped), repo.sentMotions.last().first)
        assertEquals(0, tracker.state.value!!.unsentMotions)
        assertEquals("가천대", tracker.state.value!!.view.thisStation)

        tracker.refresh()
        assertEquals("보낸 움직임은 다시 보내지 않는다", 3, repo.sentMotions.size)
    }

    @Test
    fun `서버가 거절한 움직임은 버린다(계속 막히지 않게)`() = runTest {
        tracker.start("태평", null)
        repo.motionResult = AppResult.Error(AppError.BadRequest("잘못된 시각"))
        tracker.reportMotion(Motion(MotionKind.MOVING, clock.instant()))

        tracker.refresh()

        assertEquals(1, repo.sentMotions.size)
        assertEquals(0, tracker.state.value!!.unsentMotions)
    }

    @Test
    fun `탑승이 없으면 움직임을 보내지 않는다`() = runTest {
        tracker.reportMotion(Motion(MotionKind.MOVING, clock.instant()))

        assertTrue(repo.sentMotions.isEmpty())
    }

    @Test
    fun `조용한 역 안내는 열차가 역을 떠나 이번 역이 바뀐 순간에만`() = runTest {
        fun ride(status: RideStatus, here: String?, alight: Boolean = false, id: String = "r1") =
            ActiveRide("태평", "홍대입구", view(status, here, alight = alight).copy(rideId = id), online = true, updatedAt = clock.instant())
        val moving = RideStatus.MOVING

        assertTrue("압구정로데오를 떠나 서울숲으로", ActiveRide.stationPassed(ride(RideStatus.AT_STATION, "압구정로데오"), ride(moving, "서울숲")))
        assertTrue("타는 역을 떠남", ActiveRide.stationPassed(ride(RideStatus.WAITING, "태평"), ride(moving, "가천대")))
        assertFalse("같은 역으로 가는 중 갱신", ActiveRide.stationPassed(ride(moving, "서울숲"), ride(moving, "서울숲")))
        assertFalse("역에 도착만 함", ActiveRide.stationPassed(ride(moving, "서울숲"), ride(RideStatus.AT_STATION, "서울숲")))
        assertFalse("역 사이 정차 뒤 다시 출발", ActiveRide.stationPassed(ride(RideStatus.HOLDING_BETWEEN, "서울숲"), ride(moving, "서울숲")))
        assertFalse("내릴·갈아탈 준비는 소리 있는 알림이 대신", ActiveRide.stationPassed(ride(RideStatus.AT_STATION, "서울숲"), ride(moving, "왕십리", alight = true)))
        assertFalse("위치를 모르다가 다시 찾음", ActiveRide.stationPassed(ride(RideStatus.STALE, null), ride(moving, "서울숲")))
        assertFalse("탑승 시작", ActiveRide.stationPassed(null, ride(moving, "서울숲")))
        assertFalse("새 탑승", ActiveRide.stationPassed(ride(RideStatus.AT_STATION, "선릉", id = "r1"), ride(moving, "역삼", id = "r2")))
    }

    @Test
    fun `내릴 역 알림은 켜지는 순간에만 울린다`() = runTest {
        fun ride(alight: Boolean, id: String = "r1") =
            ActiveRide("태평", "강남", view(alight = alight).copy(rideId = id), online = true, updatedAt = clock.instant())

        assertTrue(ActiveRide.alightAlertStarted(ride(false), ride(true)))
        assertTrue("시작하자마자 켜져 있어도 울린다", ActiveRide.alightAlertStarted(null, ride(true)))
        assertFalse("켜진 채 갱신되면 다시 울리지 않는다", ActiveRide.alightAlertStarted(ride(true), ride(true)))
        assertFalse(ActiveRide.alightAlertStarted(ride(true), ride(false)))
        assertFalse(ActiveRide.alightAlertStarted(ride(true), null))
        assertTrue("새 탑승이면 다시 울린다", ActiveRide.alightAlertStarted(ride(true, "r1"), ride(true, "r2")))
    }

    @Test
    fun `노선도에서 다른 열차를 고르면 그 열차로 바꾼다`() = runTest {
        tracker.start("태평", "강남")
        val other = StripTrain("6208", "이매", "ARRIVED", "왕십리", selected = false, seenSecondsAgo = 5)
        val strip = TrainStrip(RideLeg("수인분당선", "태평", "선릉"), listOf("이매", "모란", "태평"), 2, listOf(other))
        repo.stripResult = AppResult.Success(strip)
        assertEquals(AppResult.Success(strip), tracker.strip())

        repo.chooseResult = AppResult.Success(view(RideStatus.AT_STATION, "이매", line = "수인분당선").copy(trainNo = "6208"))
        tracker.choose(other, "수인분당선")
        assertEquals(listOf("6208"), repo.chosen)
        assertEquals("6208", tracker.state.value!!.view.trainNo)
    }
}
