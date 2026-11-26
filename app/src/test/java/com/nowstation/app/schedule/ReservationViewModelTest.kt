package com.nowstation.app.schedule

import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.ReservationAlarms
import com.nowstation.app.schedule.domain.ReservationStore
import com.nowstation.app.schedule.domain.Reservations
import com.nowstation.app.schedule.ui.EditorPage
import com.nowstation.app.schedule.ui.ReservationViewModel
import com.nowstation.app.schedule.ui.SaveOutcome
import com.nowstation.app.station.domain.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

class ReservationViewModelTest {
    private val taepyeong = Station("태평", listOf("수인분당선"))
    private val gangnam = Station("강남", listOf("2호선"))

    private class MemoryStore : ReservationStore {
        var saved: List<Reservation> = emptyList()
        override fun load() = saved
        override fun save(list: List<Reservation>) { saved = list }
    }

    private class FakeAlarms(var exact: Boolean = true, var notify: Boolean = true) : ReservationAlarms {
        val set = mutableMapOf<String, ZonedDateTime>()
        override fun canExact() = exact
        override fun canNotify() = notify
        override fun set(id: String, at: ZonedDateTime) { set[id] = at }
        override fun cancel(id: String) { set.remove(id) }
        override fun snooze(id: String, at: ZonedDateTime) = Unit
    }

    private val now = ZonedDateTime.of(2026, 11, 24, 7, 42, 0, 0, Reservation.ZONE)
    private val clock = Clock.fixed(now.toInstant(), Reservation.ZONE)
    private val store = MemoryStore()
    private val alarms = FakeAlarms()
    private val vm = ReservationViewModel(Reservations(store, alarms, clock), alarms, clock)

    @Test
    fun `새 예약은 바로 저장해도 지난 시각이 아니게 5분 단위로 잡는다`() {
        vm.create(taepyeong, gangnam)

        // 7:42 + 15분 = 7:57 → 7:55 (알림 7:45, 지금 뒤)
        assertEquals(LocalTime.of(7, 55), vm.editor.value!!.time)
        assertEquals(SaveOutcome.Saved, vm.save())
        assertNull(vm.editor.value)
        assertEquals(1, store.saved.size)
    }

    @Test
    fun `알람 권한이 없으면 저장하지 않고 권한을 받으러 간다`() {
        alarms.exact = false
        vm.create(taepyeong, gangnam)

        assertEquals(SaveOutcome.NeedsExactAlarm, vm.save())
        assertTrue(store.saved.isEmpty())

        alarms.exact = true // 설정에서 허용하고 돌아옴
        assertEquals(SaveOutcome.Saved, vm.save())
    }

    @Test
    fun `알림이 꺼져 있으면 알람 권한보다 먼저 알림 허용을 받으러 가고 저장하지 않는다`() {
        alarms.notify = false
        alarms.exact = false
        vm.create(taepyeong, gangnam)

        // 알람만 울리고 카드가 안 뜨는 예약을 만들지 않는다(iOS와 같음)
        assertEquals(SaveOutcome.NeedsNotification, vm.save())
        assertTrue(store.saved.isEmpty())

        alarms.notify = true // 알림을 허용하고 돌아옴 → 이어서 알람 권한
        assertEquals(SaveOutcome.NeedsExactAlarm, vm.save())
        alarms.exact = true
        assertEquals(SaveOutcome.Saved, vm.save())
    }

    @Test
    fun `한 번짜리 알릴 시각이 지났으면 저장을 막고, 요일을 고르면 저장된다`() {
        vm.create(taepyeong, gangnam)
        repeat(4) { vm.step(-5) } // 7:35 → 알림 7:25 지남

        assertTrue(vm.editor.value!!.passed)
        assertEquals(SaveOutcome.Blocked, vm.save())

        vm.showPage(EditorPage.REPEAT)
        vm.toggleDay(DayOfWeek.WEDNESDAY)
        assertEquals(SaveOutcome.Saved, vm.save())
    }

    @Test
    fun `고치면 같은 예약을 바꾼다`() {
        vm.create(taepyeong, gangnam)
        vm.save()
        val id = store.saved.single().id

        vm.edit(id)
        vm.setTime(LocalTime.of(9, 0))
        vm.save()

        assertEquals(LocalTime.of(9, 0), store.saved.single().time)
    }
}
