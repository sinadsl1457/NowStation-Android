package com.nowstation.app.core.ui

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 앱 공통 스위치(예약 목록·설정). 기본 Material 스위치는 꺼지면 손잡이가 회색 + 테두리라 "쓸 수 없는 항목"처럼 보였다(개발자 피드백)
 * → 켜짐·꺼짐 모두 흰 손잡이, 꺼짐은 회색 바탕과 작은 손잡이(Material 규칙)로만 알린다. 테두리 없음.
 */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Brand,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = OffTrack,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = Color.White,
            disabledCheckedTrackColor = Brand.copy(alpha = 0.38f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedThumbColor = Color.White,
            disabledUncheckedTrackColor = OffTrack.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** 흰 손잡이가 또렷이 보이는 가장 옅은 회색. */
private val OffTrack = Color(0xFFCBD3E1)
