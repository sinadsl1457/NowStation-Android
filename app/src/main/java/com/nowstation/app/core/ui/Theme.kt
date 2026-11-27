package com.nowstation.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.unit.sp

// 파랑: 지원 노선 색(초록·주황·노랑·빨강)과 겹치지 않는다. 주황은 3호선·수인분당선과 구분이 안 돼 쓰지 않았다
val Brand = Color(0xFF1F4FD8)
val BrandTint = Color(0xFFEAF0FF)
val Ground = Color(0xFFF4F7FC)
val Ink = Color(0xFF141B2D)
val Muted = Color(0xFF5A6478)
val Hairline = Color(0xFFE3E8F2)

/** 노선 공식 색. 서버가 주는 표시 이름(2호선 등)으로 찾고, 모르면 대표색. */
fun lineColor(line: String?): Color = when (line) {
    "2호선" -> Color(0xFF00A84D)
    "3호선" -> Color(0xFFEF7C1C)
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

/** 배지에 넣을 짧은 기호: "2호선" → "2", 그 밖의 노선은 첫 글자(수인분당선 → 수, 신분당선 → 신). */
fun lineSymbol(line: String): String = Regex("^(\\d+)호선$").find(line)?.groupValues?.get(1) ?: line.take(1)

/**
 * 노선 배지: 노선색으로 채운 원 + 흰 기호. 폭이 모두 같아서 노선이 늘어도 자리를 미리 안다
 * (이름 칩은 노선 이름 길이만큼 늘어 좁은 폰에서 깨졌다). 화면 읽기는 노선 이름으로 읽는다.
 */
@Composable
fun LineBadge(line: String, size: Dp = 24.dp) {
    val symbol = lineSymbol(line)
    Box(
        Modifier.size(size).background(lineColor(line), CircleShape).semantics { contentDescription = line },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = Color.White,
            fontSize = if (symbol.first().isDigit()) 13.sp else 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** 한 역의 노선들(홈 카드·역 검색). 배지만, [max]개를 넘으면 "+N". */
@Composable
fun LineBadges(lines: List<String>, modifier: Modifier = Modifier, max: Int = 4) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        lines.take(max).forEach { LineBadge(it) }
        if (lines.size > max) {
            Text(
                "+${lines.size - max}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Muted,
                modifier = Modifier.background(Color(0xFFEEF1F6), RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * 노선 하나를 이름까지(탑승 화면 위쪽처럼 자리가 넉넉한 곳): 배지 + 노선 이름.
 * [onDark]: 파란 배경 위(내릴 준비 화면).
 */
@Composable
fun LineChip(line: String, modifier: Modifier = Modifier, onDark: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .then(
                if (onDark) Modifier.background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                else Modifier.background(Color.White, RoundedCornerShape(50)).border(1.dp, Hairline, RoundedCornerShape(50)),
            )
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        LineBadge(line, size = 22.dp)
        Text(line, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (onDark) Color.White else Ink, maxLines = 1, softWrap = false)
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
