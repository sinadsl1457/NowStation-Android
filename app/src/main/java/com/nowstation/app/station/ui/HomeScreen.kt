package com.nowstation.app.station.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.BuildConfig
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.BrandTint
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.Hairline
import com.nowstation.app.core.ui.LocalLaunchIntro
import com.nowstation.app.core.ui.LineChip
import com.nowstation.app.core.ui.LineBadge
import com.nowstation.app.core.ui.LineBadges
import com.nowstation.app.core.ui.LineDot
import com.nowstation.app.core.ui.Muted
import com.nowstation.app.core.ui.StatusBarIcons
import com.nowstation.app.station.domain.RecentRoute
import com.nowstation.app.station.domain.Station

class HomeActions(
    val onQueryChange: (String) -> Unit,
    val onPick: (Station) -> Unit,
    val onEdit: (PickTarget) -> Unit,
    val onSwap: () -> Unit,
    val onStart: () -> Unit,
    val onUseRecent: (RecentRoute) -> Unit,
    val onCloseSearch: () -> Unit,
    val onShowGuide: () -> Unit = {},
    val onShowSettings: () -> Unit = {},
)

/** 홈. 역 검색은 칸을 눌렀을 때만 덮어서 연다(첫 화면에 긴 목록을 깔지 않음). */
@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions) {
    if (state.searching) {
        BackHandler(onBack = actions.onCloseSearch)
        StationSearchScreen(state, actions)
    } else {
        HomeContent(state, actions)
    }
}

@Composable
private fun HomeContent(state: HomeUiState, actions: HomeActions) {
    StatusBarIcons(dark = false)
    Column(Modifier.fillMaxSize().background(Ground).verticalScroll(rememberScrollState())) {
        Header(onShowSettings = actions.onShowSettings)
        // 경로 카드가 파란 머리 영역에 걸치게 위로 올린다
        val intro = LocalLaunchIntro.current
        Column(
            Modifier.offset(y = (-60).dp).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RouteCard(state, actions)
            Column(Modifier.alpha(intro?.below ?: 1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (state.recent.isNotEmpty()) RecentRoutes(state.recent, state.isStarting, actions.onUseRecent) else HowItWorks()
                if (state.lines.isNotEmpty()) SupportedLines(state.lines)
            }
            // 공공데이터 이용 조건: 출처 표시
            Text(
                "열차 위치: 서울특별시 열린데이터광장 실시간 지하철 정보",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().alpha(intro?.below ?: 1f),
            )
        }
    }
}

@Composable
private fun Header(onShowSettings: () -> Unit) {
    val intro = LocalLaunchIntro.current
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brand)
            // 첫 실행 애니메이션: 처음엔 화면 전체(시작 화면과 같은 파란 화면)였다가 원래 높이로 줄며 카드가 따라 올라온다
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                val extra = if (intro == null) 0 else
                    ((intro.windowHeight - p.height + 80.dp.roundToPx()).coerceAtLeast(0) * (1 - intro.shrink)).toInt()
                layout(p.width, p.height + extra) { p.place(0, 0) }
            },
    ) {
        Canvas(Modifier.align(Alignment.TopEnd).size(240.dp).offset(x = 40.dp, y = 30.dp).alpha(intro?.title ?: 1f)) {
            drawCircle(Color.White.copy(alpha = 0.12f), radius = size.minDimension / 2, style = Stroke(2.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.18f), radius = size.minDimension / 3.6f, style = Stroke(2.dp.toPx()), center = center.copy(x = center.x - 30.dp.toPx(), y = center.y + 20.dp.toPx()))
        }
        // 설정(사용법 다시 보기·알림·문의·개인정보처리방침)
        IconButton(
            onClick = onShowSettings,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 20.dp, end = 12.dp).alpha(intro?.title ?: 1f),
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "설정", tint = Color.White)
        }
        Column(Modifier.statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 92.dp)) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.alpha(intro?.title ?: 1f)) {
                Text("지금역", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.width(8.dp))
                // 설치된 버전을 바로 보이게: 테스트 트랙에서 새 빌드가 들어왔는지 폰에서 확인
                Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("열차에 타서 지금 탄 열차를 고르면\n잠금화면에 현재역을 보여 줘요", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f), modifier = Modifier.alpha(intro?.title ?: 1f))
            Spacer(Modifier.height(22.dp))
            TrackDecoration()
        }
    }
}

/** 노선 위 "지금 여기" 그림. */
@Composable
private fun TrackDecoration() {
    val intro = LocalLaunchIntro.current
    Canvas(
        Modifier.width(250.dp).height(24.dp)
            .onGloballyPositioned { if (intro != null) intro.track = it.boundsInRoot() }
            .alpha(intro?.handoff ?: 1f),
    ) {
        val y = size.height / 2
        val stops = listOf(0f, 0.33f, 0.66f, 1f).map { it * (size.width - 12.dp.toPx()) + 6.dp.toPx() }
        val faded = Color.White.copy(alpha = 0.45f)
        drawLine(faded, Offset(stops[0], y), Offset(stops[1], y), 3.dp.toPx())
        drawLine(Color.White, Offset(stops[1], y), Offset(stops[2], y), 3.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.55f), Offset(stops[2], y), Offset(stops[3], y), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        drawCircle(faded, 5.dp.toPx(), Offset(stops[0], y))
        drawCircle(faded, 5.dp.toPx(), Offset(stops[1], y))
        drawCircle(Color.White.copy(alpha = 0.25f), 16.dp.toPx(), Offset(stops[2], y))
        drawCircle(Color.White, 10.dp.toPx(), Offset(stops[2], y))
        drawCircle(Color.White.copy(alpha = 0.7f), 5.dp.toPx(), Offset(stops[3], y), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun RouteCard(state: HomeUiState, actions: HomeActions) {
    Column(
        Modifier
            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Brand.copy(alpha = 0.2f), spotColor = Brand.copy(alpha = 0.2f))
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StationField("타는 역", state.origin, filledDot = true, onClick = { actions.onEdit(PickTarget.ORIGIN) })
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(14.dp)) {
                    Canvas(Modifier.width(12.dp).height(14.dp)) {
                        drawLine(Color(0xFFB8C3D9), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                    }
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFEEF1F6)))
                }
                StationField("내릴 역", state.destination, filledDot = false, onClick = { actions.onEdit(PickTarget.DESTINATION) })
            }
            SwapButton(enabled = state.canSwap, onClick = actions.onSwap)
        }
        if (state.sameStation) {
            Text("타는 역과 내릴 역이 같아요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 26.dp, top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = actions.onStart,
            enabled = state.canStart,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Icon(Icons.Filled.Train, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (state.isStarting) "시작하는 중…" else "탑승 시작", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 타는 역 ↔ 내릴 역(퇴근길). 두 칸 사이 오른쪽에 둔다. */
@Composable
private fun SwapButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(start = 4.dp).size(40.dp).border(1.dp, Hairline, CircleShape),
    ) {
        Icon(Icons.Filled.SwapVert, contentDescription = "타는 역과 내릴 역 바꾸기", tint = if (enabled) Brand else Muted.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StationField(label: String, station: Station?, filledDot: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(onClick = onClick),
    ) {
        Box(
            Modifier.size(12.dp).then(
                if (filledDot) Modifier.background(Brand, CircleShape) else Modifier.border(3.dp, Brand, CircleShape),
            ),
        )
        Spacer(Modifier.width(14.dp))
        // 글자가 먼저 자리를 잡고(한 줄), 칩은 남는 자리 안에서만
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Muted, maxLines = 1, softWrap = false)
            Text(
                station?.name ?: "고르기",
                fontSize = 19.sp,
                fontWeight = if (station != null) FontWeight.Bold else FontWeight.Normal,
                color = if (station != null) MaterialTheme.colorScheme.onSurface else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.weight(1f).padding(start = 8.dp), contentAlignment = Alignment.CenterEnd) {
            station?.lines?.let { LineBadges(it) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348), modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun RecentRoutes(routes: List<RecentRoute>, starting: Boolean, onUse: (RecentRoute) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("최근 경로")
        routes.forEach { route ->
            Surface(
                onClick = { onUse(route) },
                enabled = !starting,
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Box(Modifier.size(38.dp).background(BrandTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            listOfNotNull(route.origin.name, route.destination?.name).joinToString(" → "),
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (route.origin.lines + (route.destination?.lines ?: emptyList())).distinct().forEach {
                                LineDot(it)
                                Text(it, style = MaterialTheme.typography.labelMedium, color = Muted)
                            }
                        }
                    }
                    // 누르면 위 칸에 채운다(바로 시작하지 않음). ↖ = 검색 추천어처럼 "입력칸에 넣기"(›는 다음 화면으로 보여서 뺐다)
                    Icon(Icons.Filled.NorthWest, contentDescription = "칸에 채우기", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** 최근 경로가 없을 때(처음 쓰는 사람). */
@Composable
private fun HowItWorks() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("이렇게 써요")
        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Step(1, "열차에 탄 뒤 타는 역·내릴 역을 고르고 탑승 시작")
            Step(2, "목록에서 지금 탄 열차를 골라요")
            Step(3, "잠금화면에 현재역, 내리기 전엔 진동")
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(BrandTint, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Brand)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportedLines(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("지원하는 노선")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEach { line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.background(Color.White, RoundedCornerShape(50)).border(1.dp, Hairline, RoundedCornerShape(50)).padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    LineBadge(line, size = 22.dp)
                    Text(line, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
