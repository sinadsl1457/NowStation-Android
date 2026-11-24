package com.nowstation.app.schedule.domain

import com.nowstation.app.station.domain.Station
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 예약 목록과 알람을 함께 맞춘다. 홈 화면·알람 수신·재부팅이 같이 쓰므로 앱 전체에 하나(@Singleton).
 * 목록을 바꾸면 그 자리에서 알람도 다시 건다(목록과 알람이 어긋나지 않게).
 */
@Singleton
class Reservations @Inject constructor(
    private val store: ReservationStore,
    private val alarms: ReservationAlarms,
    private val clock: Clock,
) {
    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<List<Reservation>> = _state.asStateFlow()

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock).withZoneSameInstant(Reservation.ZONE)

    fun find(id: String): Reservation? = _state.value.firstOrNull { it.id == id }

    /** 한 번짜리는 오늘만 고른다(개발자 결정). 오늘 알릴 시각이 이미 지났으면 만들 수 없다. */
    fun onceAlertPassed(time: LocalTime, leadMinutes: Int): Boolean {
        val now = now()
        val alert = now.toLocalDate().atTime(time).minusMinutes(leadMinutes.toLong()).atZone(now.zone)
        return !alert.isAfter(now)
    }

    /** 새로 만들거나([id]가 null) 고친다. 한 번짜리는 날짜를 오늘로 둔다. */
    fun save(id: String?, origin: Station, destination: Station, time: LocalTime, days: Set<DayOfWeek>, leadMinutes: Int): Reservation {
        val r = Reservation(
            id = id ?: UUID.randomUUID().toString(),
            origin = origin,
            destination = destination,
            time = time,
            days = days,
            date = if (days.isEmpty()) now().toLocalDate() else null,
            leadMinutes = leadMinutes,
        )
        write(_state.value.filterNot { it.id == r.id } + r)
        arm(r)
        return r
    }

    fun delete(id: String) {
        alarms.cancel(id)
        write(_state.value.filterNot { it.id == id })
    }

    /**
     * 켜고 끄기. 지난 한 번짜리를 다시 켜면 다음에 올 그 시각으로(오늘 지났으면 내일). iOS 알람과 같다.
     */
    fun setEnabled(id: String, on: Boolean) {
        val r = find(id) ?: return
        val next = if (on && !r.repeats) {
            val now = now()
            val today = r.alertOn(now.toLocalDate(), now.zone)
            r.copy(enabled = true, date = if (today.isAfter(now)) now.toLocalDate() else now.toLocalDate().plusDays(1))
        } else {
            r.copy(enabled = on)
        }
        replace(next)
        arm(next)
    }

    /** 알람이 울린 뒤: 반복은 다음 회차를 걸고, 한 번짜리는 끈다(목록에는 남는다). */
    fun onAlerted(id: String) {
        val r = find(id) ?: return
        if (r.repeats) {
            arm(r)
        } else {
            replace(r.copy(enabled = false))
        }
    }

    fun snooze(id: String) {
        if (find(id) == null) return
        alarms.snooze(id, now().plusMinutes(Reservation.SNOOZE_MINUTES))
    }

    /** 재부팅·시각 변경·권한 변경 뒤 모두 다시 건다(OS가 알람을 지운다). 지난 한 번짜리는 끈다. */
    fun rearmAll() {
        val now = now()
        _state.value.forEach { r ->
            when {
                !r.enabled -> alarms.cancel(r.id)
                r.nextAlert(now) == null -> {
                    alarms.cancel(r.id)
                    replace(r.copy(enabled = false))
                }
                else -> arm(r)
            }
        }
    }

    private fun arm(r: Reservation) {
        val at = r.nextAlert(now())
        if (at == null) alarms.cancel(r.id) else alarms.set(r.id, at)
    }

    private fun replace(r: Reservation) = write(_state.value.map { if (it.id == r.id) r else it })

    private fun write(list: List<Reservation>) {
        // 이른 시각부터(홈 목록 순서)
        val sorted = list.sortedWith(compareBy({ it.time }, { it.id }))
        _state.value = sorted
        store.save(sorted)
    }
}
