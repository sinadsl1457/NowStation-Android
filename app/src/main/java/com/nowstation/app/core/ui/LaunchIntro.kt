package com.nowstation.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.pow

/**
 * 처음 켤 때: OS 시작 화면의 아이콘(가운데)이 홈 머리의 노선 그림으로 바뀌며 홈이 자리 잡는다(약 1.25초).
 * 시간은 [time] 하나로 두고, 홈(머리 높이·글자 투명도)과 위 덮개([LaunchOverlay])가 같은 시간을 보고 그린다.
 */
@Stable
class LaunchIntro {
    val time = Animatable(0f)
    /** 홈 노선 그림의 자리(창 기준 px). 홈이 처음 배치될 때 알려 준다. */
    var track by mutableStateOf<Rect?>(null)
    /** 창 높이(px). 홈 머리를 처음엔 화면 전체로 늘려 시작 화면과 이어지게 한다. */
    var windowHeight by mutableIntStateOf(0)

    private val t get() = time.value
    /** 지금 역에서 퍼지는 파동(0→1). */
    val ripple get() = clamp(t / 0.45f)
    /** 아이콘 → 노선 그림(0→1). */
    val morph get() = inOut(clamp((t - 0.3f) / 0.6f))
    /** 덮개 그림에서 홈의 실제 노선 그림으로 넘겨받기(0→1). */
    val handoff get() = clamp((t - 0.8f) / 0.2f)
    /** 제목·설명(0→1). */
    val title get() = outCubic(clamp((t - 0.55f) / 0.4f))
    /** 머리가 화면 전체에서 원래 높이로 줄어듦(0→1). 아래 카드가 따라 올라온다. */
    val shrink get() = outCubic(clamp((t - 0.6f) / 0.5f))
    /** 카드 아래 내용(0→1). */
    val below get() = clamp((t - 1.0f) / 0.25f)

    companion object {
        const val TOTAL = 1.25f
        private fun clamp(x: Float) = x.coerceIn(0f, 1f)
        private fun outCubic(x: Float) = 1 - (1 - x).pow(3)
        private fun inOut(x: Float) = if (x < 0.5f) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2
    }
}

val LocalLaunchIntro = staticCompositionLocalOf<LaunchIntro?> { null }

/** 홈 위에 아이콘이 바뀌어 가는 모습을 그린다. 누르면 바로 끝낸다. */
@Composable
fun LaunchOverlay(intro: LaunchIntro, onSkip: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { intro.windowHeight = it.height }
            .pointerInput(Unit) { detectTapGestures { onSkip() } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // 아이콘 크기: 시작 화면 아이콘과 같게(배경 없는 아이콘은 288dp 칸에 108 단위 그림. 에뮬레이터에서 재서 맞춤)
            val u = 288.dp.toPx() / 108f
            val from = center
            val track = intro.track
            val m = if (track == null) 0f else intro.morph
            val to = track?.let { Offset(it.left + 163.dp.toPx(), it.top + 12.dp.toPx()) } ?: from
            val c = Offset(lerp(from.x, to.x, m), lerp(from.y, to.y, m))
            val alpha = 1f - intro.handoff

            val w = intro.ripple
            if (w > 0f && w < 1f) {
                drawCircle(Color.White.copy(alpha = 0.55f * (1 - w)), radius = 14 * u + 55.dp.toPx() * (1 - (1 - w).pow(3)), center = from, style = Stroke(1.5.dp.toPx()))
            }
            if (alpha <= 0f) return@Canvas

            val left = lerp(from.x - 25.5f * u, (track?.left ?: 0f) + 6.dp.toPx(), m)
            val right = lerp(from.x + 25.5f * u, (track?.left ?: 0f) + 244.dp.toPx(), m)
            val thick = lerp(5 * u, 3.dp.toPx(), m)
            drawRoundRect(
                Color.White.copy(alpha = alpha),
                topLeft = Offset(left, c.y - thick / 2),
                size = androidx.compose.ui.geometry.Size(right - left, thick),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(thick / 2),
            )
            val dotR = lerp(4.5f * u, 5.dp.toPx(), m)
            val leftDot = lerp(from.x - 21 * u, (track?.left ?: 0f) + 84.5.dp.toPx(), m)
            val rightDot = lerp(from.x + 21 * u, (track?.left ?: 0f) + 244.dp.toPx(), m)
            drawCircle(Color.White.copy(alpha = alpha * lerp(1f, 0.45f, m)), dotR, Offset(leftDot, c.y))
            drawCircle(Color.White.copy(alpha = alpha), dotR, Offset(rightDot, c.y))
            // 지금 역 링: 안쪽은 바탕색으로 채워 노선을 가린다(아이콘과 같게) → 끝에는 홈 그림의 옅은 테두리
            val outer = lerp(14 * u, 16.dp.toPx(), m)
            val ring = lerp(5 * u, 6.dp.toPx(), m)
            drawCircle(Brand.copy(alpha = alpha), outer - ring, c)
            drawCircle(Color.White.copy(alpha = alpha * lerp(1f, 0.25f, m)), outer - ring / 2, c, style = Stroke(ring))
            drawCircle(Color.White.copy(alpha = alpha), lerp(4.5f * u, 10.dp.toPx(), m), c)
        }
    }
}
