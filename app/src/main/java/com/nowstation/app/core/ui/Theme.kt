package com.nowstation.app.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brand = Color(0xFF356DF3)

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
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F3F4),
            onSurfaceVariant = Color(0xFF5F6368),
        ),
        content = content,
    )
}
