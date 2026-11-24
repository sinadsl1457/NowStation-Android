package com.nowstation.app.schedule.domain

import com.nowstation.app.station.domain.Station
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 출발 예약: 나갈 시각 [leadMinutes]분 전에 알린다.
 * [days]가 비면 한 번([date] 하루)이고, 있으면 그 요일마다 끌 때까지(종료일 없음, 개발자 결정).
 */
data class Reservation(
    val id: String,
    val origin: Station,
    val destination: Station,
    /** 5분 단위(시계판이 5분 단위만 고른다). */
    val time: LocalTime,
    val days: Set<DayOfWeek>,
    /** 한 번짜리의 날짜. 반복이면 쓰지 않는다. */
    val date: LocalDate?,
    val leadMinutes: Int,
    val enabled: Boolean = true,
) {
    val repeats: Boolean get() = days.isNotEmpty()

    /**
     * 다음에 알릴 시각. 알릴 시각이 이미 지났으면 그 회차는 건너뛴다(늦게 울린 알람이 같은 회차를 또 울리지 않게).
     * 꺼져 있거나 한 번짜리가 지났으면 null.
     */
    fun nextAlert(now: ZonedDateTime): ZonedDateTime? {
        if (!enabled) return null
        val zone = now.zone
        if (!repeats) {
            val day = date ?: return null
            return alertOn(day, zone).takeIf { it.isAfter(now) }
        }
        // 알림이 전날로 넘어갈 수 있어(0:05 출발, 10분 전) 어제부터 본다
        return (-1L..7L).asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { it.dayOfWeek in days }
            .map { alertOn(it, zone) }
            .firstOrNull { it.isAfter(now) }
    }

    /** 그 날 나갈 시각 기준 알림 시각. */
    fun alertOn(day: LocalDate, zone: ZoneId): ZonedDateTime =
        LocalDateTime.of(day, time).minusMinutes(leadMinutes.toLong()).atZone(zone)

    companion object {
        val LEADS = listOf(5, 10, 15, 20)
        const val DEFAULT_LEAD = 10
        const val SNOOZE_MINUTES = 5L
        const val STEP_MINUTES = 5

        /** 지하철 시간이라 서울 시각으로 고정(폰 시간대를 바꿔도 출발 시각은 그대로). */
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

/** 반복 요일을 줄로 보여 줄 때: 안 함 / 매일 / 주중 / 주말 / 화~토 / 월 수 금. iOS 알람과 같은 묶음. */
object RepeatLabel {
    /** 월요일부터(한국 달력 기준 아니고 출근 기준: 평일이 먼저). */
    val ORDER = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
    )
    private val WEEKDAYS = ORDER.take(5).toSet()
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun short(day: DayOfWeek) = "월화수목금토일"[ORDER.indexOf(day)].toString()

    fun of(days: Set<DayOfWeek>): String {
        val picked = ORDER.filter { it in days }
        return when {
            picked.isEmpty() -> "안 함"
            picked.size == 7 -> "매일"
            days == WEEKDAYS -> "주중"
            days == WEEKEND -> "주말"
            // 3일 이상 이어지면 범위로(화~토), 아니면 나열
            picked.size >= 3 && picked.zipWithNext().all { (a, b) -> ORDER.indexOf(b) == ORDER.indexOf(a) + 1 } ->
                "${short(picked.first())}~${short(picked.last())}"
            else -> picked.joinToString(" ") { short(it) }
        }
    }
}

/** 예약 목록을 폰 안에 둔다(서버로 보내지 않음). */
interface ReservationStore {
    fun load(): List<Reservation>
    fun save(list: List<Reservation>)
}

/**
 * 저장하는 순간 오늘 곧 나갈 예약을 잠금화면에 계속 띄운다(iOS Live Activity와 같은 규칙: 오늘, 8시간 안).
 * 남은 시간은 시스템이 줄여 그린다.
 */
interface ReservationLiveDisplay {
    fun showCard(r: Reservation, departure: ZonedDateTime)
    fun hideCard(id: String)

    companion object {
        const val MAX_HOURS = 8L

        /** 오늘 회차의 나갈 시각이 지금부터 8시간 안이면 그 시각. 아니면 null(띄우지 않는다). */
        fun departureToShow(r: Reservation, now: ZonedDateTime): ZonedDateTime? {
            if (!r.enabled) return null
            val today = now.toLocalDate()
            if (r.repeats) {
                if (now.dayOfWeek !in r.days) return null
            } else if (r.date != today) {
                return null
            }
            val departure = today.atTime(r.time).atZone(now.zone)
            return departure.takeIf { it.isAfter(now) && !it.isAfter(now.plusHours(MAX_HOURS)) }
        }
    }
}

/** 정해진 시각에 깨워 달라는 부탁. 구현은 AlarmManager. */
interface ReservationAlarms {
    /** 나갈 시각 직전 알림을 정확히 울리려면 "알람 및 리마인더" 권한이 필요하다(Android 12+). */
    fun canExact(): Boolean
    fun set(id: String, at: ZonedDateTime)
    fun cancel(id: String)
    /** "5분 뒤 다시": 예약의 다음 회차와 따로 한 번 더. */
    fun snooze(id: String, at: ZonedDateTime)
}

/** 오전 8:10 / 오후 6:40. 예약 화면·목록·알림이 같이 쓴다. */
object TimeText {
    fun of(t: LocalTime): String {
        val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
        return "${if (t.hour < 12) "오전" else "오후"} $h12:${"%02d".format(t.minute)}"
    }
}
