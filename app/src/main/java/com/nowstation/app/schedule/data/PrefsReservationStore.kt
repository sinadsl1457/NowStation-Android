package com.nowstation.app.schedule.data

import android.content.SharedPreferences
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.ReservationStore
import com.nowstation.app.station.domain.Station
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** 목록 전체를 한 키에 JSON으로(반쪽 저장 없음). 읽지 못하면 빈 목록. */
class PrefsReservationStore @Inject constructor(private val prefs: SharedPreferences) : ReservationStore {

    override fun load(): List<Reservation> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ReservationJson>>(raw).map { it.toDomain() } }.getOrDefault(emptyList())
    }

    // commit: 알람 수신기는 곧 프로세스가 끝날 수 있어 바로 쓴다
    override fun save(list: List<Reservation>) {
        prefs.edit().putString(KEY, json.encodeToString(list.map { ReservationJson.of(it) })).commit()
    }

    @Serializable
    private data class StationJson(val name: String, val lines: List<String>)

    @Serializable
    private data class ReservationJson(
        val id: String,
        val origin: StationJson,
        val destination: StationJson,
        /** "08:10" */
        val time: String,
        /** 1=월 … 7=일 */
        val days: List<Int>,
        val date: String? = null,
        val lead: Int,
        val enabled: Boolean,
    ) {
        fun toDomain() = Reservation(
            id = id,
            origin = Station(origin.name, origin.lines),
            destination = Station(destination.name, destination.lines),
            time = LocalTime.parse(time),
            days = days.map(DayOfWeek::of).toSet(),
            date = date?.let(LocalDate::parse),
            leadMinutes = lead,
            enabled = enabled,
        )

        companion object {
            fun of(r: Reservation) = ReservationJson(
                id = r.id,
                origin = StationJson(r.origin.name, r.origin.lines),
                destination = StationJson(r.destination.name, r.destination.lines),
                time = r.time.toString(),
                days = r.days.map { it.value }.sorted(),
                date = r.date?.toString(),
                lead = r.leadMinutes,
                enabled = r.enabled,
            )
        }
    }

    private companion object {
        const val KEY = "reservations_v1"
        val json = Json { ignoreUnknownKeys = true }
    }
}
