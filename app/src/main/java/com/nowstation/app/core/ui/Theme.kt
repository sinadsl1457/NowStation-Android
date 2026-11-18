package com.nowstation.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 파랑: 지원 노선 색(초록·노랑·빨강)과 겹치지 않는다. 주황은 수인분당선과 구분이 안 돼 쓰지 않았다
val Brand = Color(0xFF1F4FD8)
val BrandTint = Color(0xFFEAF0FF)
val Ground = Color(0xFFF4F7FC)
val Ink = Color(0xFF141B2D)
val Muted = Color(0xFF5A6478)
val Hairline = Color(0xFFE3E8F2)

/** 노선 공식 색. 서버가 주는 표시 이름(2호선 등)으로 찾고, 모르면 대표색. */
fun lineColor(line: String?): Color = when (line) {
    "2호선" -> Color(0xFF00A84D)
    "수인분당선" -> Color(0xFFF5A200)
    "신분당선" -> Color(0xFFD4003B)
    else -> Brand
}

/** 지하철 안 밝은 조명에서도 읽기 쉽게 항상 라이트. */
@Composable
fun NowStationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brand,
            onPrimary = Color.White,
            primaryContainer = BrandTint,
            onPrimaryContainer = Brand,
            background = Ground,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Color(0xFFF1F4FA),
            onSurfaceVariant = Muted,
            outline = Color(0xFFD5DCE8),
            outlineVariant = Hairline,
        ),
        content = content,
    )
}

/**
 * 노선 표시: 흰 바탕에 노선 색 점. 노선 색 바탕에 흰 글씨는 노랑(수인분당선)·초록(2호선)에서 글자가 잘 안 읽힌다.
 * [onDark]: 파란 배경 위(내릴 준비 화면).
 */
@Composable
fun LineChip(line: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .then(
                if (onDark) Modifier.background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                else Modifier.background(Color.White, RoundedCornerShape(50)).border(1.dp, Hairline, RoundedCornerShape(50)),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        LineDot(line)
        Text(line, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (onDark) Color.White else Ink)
    }
}

@Composable
fun LineDot(line: String, size: Int = 8) {
    Box(Modifier.size(size.dp).background(lineColor(line), CircleShape))
}

/** 화면 맨 위 시계·배터리 아이콘 색. [dark] = 검은 아이콘(밝은 바탕), false = 흰 아이콘(파란 바탕). */
@Composable
fun StatusBarIcons(dark: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (view.isInEditMode) return
    androidx.compose.runtime.SideEffect {
        val window = (view.context as android.app.Activity).window
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = dark
    }
}
