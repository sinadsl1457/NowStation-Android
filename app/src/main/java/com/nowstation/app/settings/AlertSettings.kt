package com.nowstation.app.settings

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 안 알림 스위치. 진행 중 알림(잠금화면 현재역)은 탑승을 따라가는 서비스가 반드시 띄워야 해서(OS 규칙) 여기서 끄지 않는다. */
data class AlertPrefs(
    /** 갈아탈 역 한 정거장 전 팝업. */
    val transfer: Boolean = true,
    /** 내릴 역 한 정거장 전 팝업. */
    val alight: Boolean = true,
    /** 위 알림 때 진동(알람 용도라 무음 모드에서도 울림). */
    val vibrate: Boolean = true,
    /** 역을 떠날 때마다 소리·진동 없이 잠깐 뜨는 배너(가로로 영상을 볼 때 다이나믹 아일랜드·상태 표시줄 칩 대신). */
    val station: Boolean = true,
)

@Singleton
class AlertSettings @Inject constructor(private val prefs: SharedPreferences) {
    private val _state = MutableStateFlow(
        AlertPrefs(
            transfer = prefs.getBoolean(TRANSFER, true),
            alight = prefs.getBoolean(ALIGHT, true),
            vibrate = prefs.getBoolean(VIBRATE, true),
            station = prefs.getBoolean(STATION, true),
        ),
    )
    val state: StateFlow<AlertPrefs> = _state.asStateFlow()

    fun update(change: (AlertPrefs) -> AlertPrefs) {
        val next = change(_state.value)
        _state.value = next
        prefs.edit().putBoolean(TRANSFER, next.transfer).putBoolean(ALIGHT, next.alight).putBoolean(VIBRATE, next.vibrate).putBoolean(STATION, next.station).apply()
    }

    private companion object {
        const val TRANSFER = "alert_transfer"
        const val ALIGHT = "alert_alight"
        const val VIBRATE = "alert_vibrate"
        const val STATION = "alert_station"
    }
}
