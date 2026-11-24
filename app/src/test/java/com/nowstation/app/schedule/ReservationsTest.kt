package com.nowstation.app.schedule

import com.nowstation.app.schedule.domain.RepeatLabel
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.ReservationAlarms
import com.nowstation.app.schedule.domain.ReservationStore
import com.nowstation.app.schedule.domain.Reservations
import com.nowstation.app.station.domain.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime
import java.time.ZonedDateTime

class ReservationsTest {
    private val taepyeong = Station("태평", listOf("수인분당선"))
    private val gangnam = Station("강남", listOf("2호선"))

    /** 2026-11-24 화요일 서울 시각. */
    private fun at(hhmm: String, day: Int = 24) = ZonedDateTime.of(2026, 11, day, hhmm.take(2).toInt(), hhmm.takeLast(2).toInt(), 0, 0, Reservation.ZONE)
    private fun clockAt(t: ZonedDateTime) = Clock.fixed(t.toInstant(), Reservation.ZONE)

    private class MemoryStore : ReservationStore {
        var saved: List<Reservation> = emptyList()
        override fun load() = saved
        override fun save(list: List<Reservation>) { saved = list }
    }

    private class FakeAlarms : ReservationAlarms {
        val set = mutableMapOf<String, ZonedDateTime>()
        val snoozed = mutableMapOf<String, ZonedDateTime>()
        override fun canExact() = true
        override fun set(id: String, at: ZonedDateTime) { set[id] = at }
        override fun cancel(id: String) { set.remove(id) }
        override fun snooze(id: String, at: ZonedDateTime) { snoozed[id] = at }
    }

    private fun reservations(now: ZonedDateTime, store: MemoryStore = MemoryStore(), alarms: FakeAlarms = FakeAlarms()) =
        Triple(Reservations(store, alarms, clockAt(now)), store, alarms)

    @Test
    fun `한 번짜리는 오늘 나갈 시각 10분 전에 건다`() {
        val (r, _, alarms) = reservations(at("0742"))
        val saved = r.save(null, taepyeong, gangnam, LocalTime.of(8, 10), emptySet(), 10)

        assertEquals(at("0800"), alarms.set[saved.id])
        assertEquals(at("0742").toLocalDate(), saved.date)
    }

    @Test
    fun `오늘 알릴 시각이 지났거나 지금이면 한 번짜리를 만들 수 없다`() {
        val (r, _, _) = reservations(at("0805"))

        assertTrue(r.onceAlertPassed(LocalTime.of(8, 10), 10)) // 8:00 지남
        assertTrue(r.onceAlertPassed(LocalTime.of(8, 10), 5)) // 8:05 = 지금
        assertFalse(r.onceAlertPassed(LocalTime.of(8, 20), 10))
    }

    @Test
    fun `반복은 오늘 시각이 지났으면 다음 고른 요일로`() {
        // 화요일 10:30, 화~토 10:00 → 오늘(화)은 9:50이 지났으니 수요일 9:50
        val (r, _, alarms) = reservations(at("1030"))
        val saved = r.save(null, taepyeong, gangnam, LocalTime.of(10, 0), setOf(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY), 10)

        assertEquals(at("0950", day = 25), alarms.set[saved.id])
    }

    @Test
    fun `반복은 울린 뒤 다음 회차를 건다`() {
        // 토요일 9:50에 울림 → 일·월은 안 고름 → 다음 화요일
        val store = MemoryStore()
        val alarms = FakeAlarms()
        val saved = reservations(at("0900", day = 28), store, alarms).first
            .save(null, taepyeong, gangnam, LocalTime.of(10, 0), setOf(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY), 10)

        reservations(at("0950", day = 28), store, alarms).first.onAlerted(saved.id)

        assertEquals(ZonedDateTime.of(2026, 12, 1, 9, 50, 0, 0, Reservation.ZONE), alarms.set[saved.id])
    }

    @Test
    fun `한 번짜리는 울린 뒤 꺼지고 목록에 남는다`() {
        val store = MemoryStore()
        val alarms = FakeAlarms()
        val saved = reservations(at("0742"), store, alarms).first.save(null, taepyeong, gangnam, LocalTime.of(8, 10), emptySet(), 10)

        val later = reservations(at("0800"), store, alarms).first
        later.onAlerted(saved.id)

        assertFalse(later.find(saved.id)!!.enabled)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun `지난 한 번짜리를 다시 켜면 오늘 지났으니 내일로`() {
        val store = MemoryStore()
        val alarms = FakeAlarms()
        val saved = reservations(at("0742"), store, alarms).first.save(null, taepyeong, gangnam, LocalTime.of(8, 10), emptySet(), 10)
        val later = reservations(at("0900"), store, alarms).first
        later.onAlerted(saved.id)

        later.setEnabled(saved.id, true)

        assertEquals(at("0800", day = 25), alarms.set[saved.id])
    }

    @Test
    fun `끄면 알람을 지운다`() {
        val (r, _, alarms) = reservations(at("0742"))
        val saved = r.save(null, taepyeong, gangnam, LocalTime.of(8, 10), emptySet(), 10)

        r.setEnabled(saved.id, false)

        assertNull(alarms.set[saved.id])
    }

    @Test
    fun `재부팅 뒤 다시 걸 때 지난 한 번짜리는 끈다`() {
        val store = MemoryStore()
        val alarms = FakeAlarms()
        val saved = reservations(at("0742"), store, alarms).first.save(null, taepyeong, gangnam, LocalTime.of(8, 10), emptySet(), 10)
        alarms.set.clear() // 재부팅으로 OS가 지움

        val booted = reservations(at("1200"), store, alarms).first
        booted.rearmAll()

        assertFalse(booted.find(saved.id)!!.enabled)
        assertNull(alarms.set[saved.id])
    }

    @Test
    fun `5분 뒤 다시는 예약의 다음 회차와 따로 건다`() {
        val (r, _, alarms) = reservations(at("0800"))
        val saved = r.save(null, taepyeong, gangnam, LocalTime.of(8, 30), emptySet(), 10)

        r.snooze(saved.id)

        assertEquals(at("0805"), alarms.snoozed[saved.id])
        assertEquals(at("0820"), alarms.set[saved.id])
    }

    @Test
    fun `자정 직후 출발이면 알림은 전날 밤`() {
        // 화요일 23:00, 매일 0:05 출발 10분 전 → 화요일 23:55
        val (r, _, alarms) = reservations(at("2300"))
        val saved = r.save(null, taepyeong, gangnam, LocalTime.of(0, 5), RepeatLabel.ORDER.toSet(), 10)

        assertEquals(at("2355"), alarms.set[saved.id])
    }

    @Test
    fun `반복 요일 표시`() {
        assertEquals("안 함", RepeatLabel.of(emptySet()))
        assertEquals("매일", RepeatLabel.of(RepeatLabel.ORDER.toSet()))
        assertEquals("주중", RepeatLabel.of(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)))
        assertEquals("주말", RepeatLabel.of(setOf(SATURDAY, SUNDAY)))
        assertEquals("화~토", RepeatLabel.of(setOf(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)))
        assertEquals("월 수 금", RepeatLabel.of(setOf(MONDAY, WEDNESDAY, FRIDAY)))
        assertEquals("월 화", RepeatLabel.of(setOf(MONDAY, TUESDAY)))
    }

    @Test
    fun `나갈 시각까지 5분이 채 안 남았으면 5분 뒤 다시를 뺀다`() {
        val departure = at("0810")
        assertTrue(Reservation.canSnooze(departure, at("0800"))) // 10분 전
        assertTrue(Reservation.canSnooze(departure, at("0805").plusNanos(400_000_000))) // 5분 전 알림이 0.4초 늦게 깨어남
        assertFalse(Reservation.canSnooze(departure, at("0806"))) // 4분 남음
        assertFalse(Reservation.canSnooze(departure, at("0810")))
    }
}
