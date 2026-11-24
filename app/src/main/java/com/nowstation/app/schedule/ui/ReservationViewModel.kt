package com.nowstation.app.schedule.ui

import androidx.lifecycle.ViewModel
import com.nowstation.app.schedule.domain.RepeatLabel
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.ReservationAlarms
import com.nowstation.app.schedule.domain.Reservations
import com.nowstation.app.schedule.domain.TimeText
import com.nowstation.app.station.domain.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** 예약 시트 안에서 보이는 쪽: 본 화면 또는 반복 요일 고르기. */
enum class EditorPage { MAIN, REPEAT }

data class EditorState(
    /** null이면 새 예약. */
    val id: String?,
    val origin: Station,
    val destination: Station,
    val time: LocalTime,
    val days: Set<DayOfWeek> = emptySet(),
    val leadMinutes: Int = Reservation.DEFAULT_LEAD,
    val page: EditorPage = EditorPage.MAIN,
    val dialOpen: Boolean = false,
    /** 한 번짜리인데 오늘 알릴 시각이 지남 → 저장 막음. */
    val passed: Boolean = false,
) {
    val canSave: Boolean get() = days.isNotEmpty() || !passed
}

/** 저장을 눌렀을 때 화면이 할 일. */
sealed interface SaveOutcome {
    data object Saved : SaveOutcome
    /** "알람 및 리마인더" 권한을 받으러 설정 화면으로(개발자 결정: 처음 예약할 때 받는다). */
    data object NeedsExactAlarm : SaveOutcome
    data object Blocked : SaveOutcome
}

/** 홈의 예약 목록과 예약 시트. 목록은 앱 전체의 [Reservations]를 보여 주고, 시트 상태는 이 ViewModel이 갖는다. */
@HiltViewModel
class ReservationViewModel @Inject constructor(
    private val reservations: Reservations,
    private val alarms: ReservationAlarms,
    private val clock: Clock,
) : ViewModel() {

    val list: StateFlow<List<Reservation>> = reservations.state

    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor.asStateFlow()

    /** 새 예약: 지금보다 뒤의 가장 가까운 5분 + 미리 알림만큼(바로 저장해도 지난 시각이 아니게). */
    fun create(origin: Station, destination: Station) {
        val now = ZonedDateTime.now(clock).withZoneSameInstant(Reservation.ZONE).toLocalTime()
        val start = now.plusMinutes((Reservation.DEFAULT_LEAD + Reservation.STEP_MINUTES).toLong())
        val rounded = start.withSecond(0).withNano(0).let { it.withMinute(it.minute / Reservation.STEP_MINUTES * Reservation.STEP_MINUTES) }
        open(EditorState(id = null, origin = origin, destination = destination, time = rounded))
    }

    fun edit(id: String) {
        val r = reservations.find(id) ?: return
        open(EditorState(id = r.id, origin = r.origin, destination = r.destination, time = r.time, days = r.days, leadMinutes = r.leadMinutes))
    }

    fun close() = _editor.update { null }

    fun step(minutes: Int) = change { it.copy(time = it.time.plusMinutes(minutes.toLong())) }
    fun setTime(time: LocalTime) = change { it.copy(time = time, dialOpen = false) }
    fun openDial(open: Boolean) = change { it.copy(dialOpen = open) }
    fun setLead(minutes: Int) = change { it.copy(leadMinutes = minutes) }
    fun showPage(page: EditorPage) = change { it.copy(page = page) }
    fun toggleDay(day: DayOfWeek) = change { it.copy(days = if (day in it.days) it.days - day else it.days + day) }

    fun save(): SaveOutcome {
        val e = _editor.value ?: return SaveOutcome.Blocked
        if (!e.canSave) return SaveOutcome.Blocked
        if (!alarms.canExact()) return SaveOutcome.NeedsExactAlarm
        reservations.save(e.id, e.origin, e.destination, e.time, e.days, e.leadMinutes)
        close()
        return SaveOutcome.Saved
    }

    fun delete() {
        val id = _editor.value?.id ?: return
        reservations.delete(id)
        close()
    }

    fun setEnabled(id: String, on: Boolean) = reservations.setEnabled(id, on)

    private fun open(e: EditorState) = _editor.update { withPassed(e) }

    private fun change(edit: (EditorState) -> EditorState) = _editor.update { it?.let { e -> withPassed(edit(e)) } }

    private fun withPassed(e: EditorState) = e.copy(passed = reservations.onceAlertPassed(e.time, e.leadMinutes))
}

/** 예약 시트·홈 목록이 같이 쓰는 문구. */
object ReservationTexts {
    /** "평일마다 오전 8:10에 나가요. 오전 8:00에 알려 드려요. 오늘부터 끌 때까지 계속돼요." */
    fun summary(time: LocalTime, days: Set<DayOfWeek>, lead: Int, passed: Boolean): String {
        val at = TimeText.of(time)
        val alert = TimeText.of(time.minusMinutes(lead.toLong()))
        if (days.isEmpty()) {
            return if (passed) "알릴 시각($alert)이 이미 지났어요. 더 늦은 시각을 골라 주세요."
            else "오늘 ${at}에 나가요. ${alert}에 알려 드려요."
        }
        val label = RepeatLabel.of(days)
        val whenText = when (label) {
            "매일" -> "매일"
            "주중" -> "평일마다"
            "주말" -> "주말마다"
            else -> "매주 ${label.replace(" ", "·")}"
        }
        return "$whenText ${at}에 나가요. ${alert}에 알려 드려요. 오늘부터 끌 때까지 계속돼요."
    }

    /** 홈 목록 한 줄 아래 설명: "한 번 · 10분 전에 알려요" / "반복 · 10분 전에 알려요". */
    fun detail(r: Reservation) = "${if (r.repeats) "반복" else "한 번"} · ${r.leadMinutes}분 전에 알려요"

    /** 홈 목록 시각 위 작은 글자: 오늘 / 내일 / 주중 / 화~토. 지난 한 번짜리를 다시 켜면 내일이 될 수 있다. */
    fun dayLabel(r: Reservation, today: LocalDate): String = when {
        r.repeats -> RepeatLabel.of(r.days)
        r.date == today.plusDays(1) -> "내일"
        else -> "오늘"
    }
}
