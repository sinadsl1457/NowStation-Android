package com.nowstation.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 앱 공통 스위치(예약 목록·설정). 직접 그린다: Material 스위치는 꺼지면 손잡이를 작게(16dp) 그려 "쓸 수 없는 항목"처럼 보였다(개발자 피드백).
 * 켜짐·꺼짐 모두 같은 크기의 흰 손잡이(옅은 그림자), 상태는 손잡이 위치와 바탕색(파랑·회색)으로만 알린다(iOS 방식).
 * 누르는 자리는 48dp, 화면 낭독기에는 "스위치"로 읽힌다.
 */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val thumbX by animateDpAsState(if (checked) TrackWidth - ThumbSize - Inset * 2 else 0.dp, Slide, label = "thumb")
    val track by animateColorAsState(if (checked) Brand else OffTrack, label = "track")
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(TrackWidth, TrackHeight).background(track, CircleShape).padding(Inset),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.offset(x = thumbX).size(ThumbSize)
                    .shadow(2.dp, CircleShape, ambientColor = Shade, spotColor = Shade)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

private val TrackWidth = 52.dp
private val TrackHeight = 32.dp
private val Inset = 3.dp
private val ThumbSize = TrackHeight - Inset * 2
/** 흰 손잡이가 또렷이 보이는 가장 옅은 회색. */
private val OffTrack = Color(0xFFCBD3E1)
private val Shade = Color(0x55141B2D)
private val Slide = spring<Dp>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
