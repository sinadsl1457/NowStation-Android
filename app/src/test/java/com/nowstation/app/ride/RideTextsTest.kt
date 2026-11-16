package com.nowstation.app.ride

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
}
