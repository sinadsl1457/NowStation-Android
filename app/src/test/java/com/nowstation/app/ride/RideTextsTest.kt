package com.nowstation.app.ride

import com.nowstation.app.ride.domain.RideNoticeKind
import com.nowstation.app.ride.domain.StripTrain
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideStatus
import com.nowstation.app.ride.domain.RideView
import com.nowstation.app.ride.ui.RideTexts
import com.nowstation.app.testutil.view
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RideTextsTest {
    private fun ride(v: RideView, online: Boolean = true) = ActiveRide("태평", "강남", v, online, Instant.EPOCH)

    @Test
    fun `기다리는 중`() {
        val r = ride(view(RideStatus.WAITING, "태평"))
        assertEquals("태평역에서 기다리는 중", RideTexts.title(r))
        assertEquals("열차에 타면 자동으로 따라가요", RideTexts.detail(r))
    }

    @Test
    fun `정차 중과 이동 중`() {
        assertEquals("정차 중 · 다음 역 선릉", RideTexts.detail(ride(view(RideStatus.AT_STATION, "한티", "선릉"))))
        val moving = ride(view(RideStatus.MOVING, "역삼", "강남"))
        assertEquals("이번 역 역삼", RideTexts.title(moving))
        assertEquals("다음 역 강남", RideTexts.detail(moving))
    }

    @Test
    fun `내릴 역 직전에는 제목으로 알린다`() {
        assertEquals("곧 강남, 내릴 준비하세요", RideTexts.title(ride(view(RideStatus.MOVING, "강남", "교대", alight = true))))
    }

    @Test
    fun `연결이 끊기면 덧붙인다, 소식이 없으면 위치 확인 중`() {
        assertEquals("다음 역 복정 · 연결 끊김(다시 시도 중)", RideTexts.detail(ride(view(RideStatus.MOVING, "가천대", "복정"), online = false)))
        assertEquals("위치 확인 중", RideTexts.title(ride(view(RideStatus.STALE, "가천대"))))
    }

    @Test
    fun `상태 표시줄 칩은 이번 역 이름만, 소식이 없으면 비운다`() {
        assertEquals("대기", RideTexts.chip(ride(view(RideStatus.WAITING, "태평"))))
        assertEquals("역삼", RideTexts.chip(ride(view(RideStatus.MOVING, "역삼", "강남"))))
        assertEquals("한티", RideTexts.chip(ride(view(RideStatus.HOLDING_BETWEEN, "한티", "선릉"))))
        assertEquals(null, RideTexts.chip(ride(view(RideStatus.STALE, "역삼"))))
    }

    @Test
    fun `상태 표시는 연결 끊김이 먼저`() {
        assertEquals("달리는 중", RideTexts.status(ride(view(RideStatus.MOVING, "역삼", "강남"))))
        assertEquals("역 사이 정차", RideTexts.status(ride(view(RideStatus.HOLDING_BETWEEN, "역삼", "강남"))))
        assertEquals("연결 끊김", RideTexts.status(ride(view(RideStatus.MOVING, "역삼", "강남"), online = false)))
    }

    @Test
    fun `안내 띠와 남은 정거장 문구는 iOS와 같다`() {
        fun r(v: com.nowstation.app.ride.domain.RideView) = ride(v)
        val base = view(RideStatus.AT_STATION, "모란")
        assertEquals("6206 열차를 골라 뒀어요. 다른 열차면 아래에서 눌러 주세요", RideTexts.banner(r(base.copy(trainNo = "6206"))))
        assertEquals("6206 열차 탑승 확인", RideTexts.banner(r(base.copy(trainNo = "6206", confirmed = true))))
        assertEquals("열차를 놓치신 것 같아요. 다음 열차(6208)로 바꿨어요", RideTexts.banner(r(base.copy(trainNo = "6208", notice = RideNoticeKind.MISSED))))
        assertEquals("선릉까지 6정거장", RideTexts.remaining(r(base.copy(target = "선릉", remainingStops = 6))))
        assertEquals("왕십리행", RideTexts.terminal(StripTrain("6206", "모란", "ARRIVED", "왕십리", true, 3)))
    }
}
