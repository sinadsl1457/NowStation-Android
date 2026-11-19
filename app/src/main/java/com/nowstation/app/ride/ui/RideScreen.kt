package com.nowstation.app.ride.ui

import com.nowstation.app.ride.domain.TrainStrip
import com.nowstation.app.ride.domain.StripTrain
import com.nowstation.app.ride.domain.RideLeg
import com.nowstation.app.core.ui.lineColor
import com.nowstation.app.core.ui.LineBadge
import com.nowstation.app.core.ui.Ink
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.BrandTint
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.LineChip
import com.nowstation.app.core.ui.Muted
import com.nowstation.app.core.ui.StatusBarIcons
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 탑승 중. 내릴 역 직전이면 화면 전체를 파랑으로 바꿔 주머니에서 꺼냈을 때 바로 알아보게 한다. */
@Composable
fun RideScreen(
    ride: ActiveRide,
    strip: TrainStrip? = null,
    picking: String? = null,
    onPick: (StripTrain) -> Unit = {},
    onEnd: () -> Unit,
) {
    // 끝내기는 되돌릴 수 없어서(현재역 표시·내릴 역 알림이 멈춤) 한 번 더 묻는다
    var confirming by rememberSaveable { mutableStateOf(false) }
    val askEnd = { confirming = true }
    AnimatedContent(
        targetState = ride.view.prepareToAlight,
        transitionSpec = { (slideInVertically(tween(450)) { it / 3 } + fadeIn(tween(300))) togetherWith fadeOut(tween(200)) },
        label = "alight",
    ) { alight ->
        if (alight) AlightContent(ride, askEnd) else RidingContent(ride, strip, picking, onPick, askEnd)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("탑승을 끝낼까요?") },
            text = { Text("끝내면 현재역 표시와 내릴 역 알림이 멈춰요.") },
            confirmButton = {
                TextButton(onClick = { confirming = false; onEnd() }) { Text("끝내기", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("계속 타기", color = Muted) } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun RidingContent(ride: ActiveRide, strip: TrainStrip?, picking: String?, onPick: (StripTrain) -> Unit, onEnd: () -> Unit) {
    StatusBarIcons(dark = true)
    val v = ride.view
    val waiting = v.status == RideStatus.WAITING
    Column(
        Modifier.fillMaxSize().background(Ground).safeDrawingPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            v.line?.let { LineChip(it) }
            Spacer(Modifier.weight(1f))
            StatusPill(ride)
        }

        Banner(ride)

        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(22.dp)) {
            Text(
                when {
                    waiting -> "기다리는 역"
                    v.thisStation == null -> "위치 확인 중"
                    else -> "이번 정거장은"
                },
                style = MaterialTheme.typography.titleSmall, color = Muted,
            )
            Text(v.thisStation ?: ride.origin, fontSize = 60.sp, lineHeight = 70.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(RideTexts.remaining(ride) ?: RideTexts.detail(ride), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A3348))
        }

        if (v.legs.isNotEmpty()) LegChips(v.legs, v.line)
        if (strip != null) StripCard(strip, picking, onPick) else RouteCard(ride)

        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("화면을 꺼도 잠금화면에서 계속 보여요", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        OutlinedButton(
            onClick = onEnd,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text("탑승 끝내기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348)) }
    }
}

@Composable
private fun StatusPill(ride: ActiveRide) {
    val live = ride.online && ride.view.status == RideStatus.MOVING
    val color = if (ride.online) Brand else Muted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(if (ride.online) BrandTint else Color(0xFFE9ECF2), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // 달리는 중이면 점이 숨 쉬듯 깜빡인다
        val pulse = if (live) {
            val t = rememberInfiniteTransition(label = "live")
            t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot").value
        } else 1f
        Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse }.background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(RideTexts.status(ride), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

/** 타는 역 → 내릴 역. 노선도(앞뒤 역)는 서버가 역 목록을 줄 때 붙인다. */
@Composable
private fun RouteCard(ride: ActiveRide) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(horizontal = 22.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(Color(0xFFC9D1E0), CircleShape))
            Spacer(Modifier.width(16.dp))
            Text(ride.origin, style = MaterialTheme.typography.bodyLarge, color = Muted, modifier = Modifier.weight(1f))
            ride.startedAt?.let { Text("${timeFormat.format(it)} 탑승", style = MaterialTheme.typography.labelMedium, color = Muted) }
        }
        Canvas(Modifier.padding(start = 5.dp).width(2.dp).height(26.dp)) {
            drawLine(Color(0xFFB8C3D9), center.copy(y = 0f), center.copy(y = size.height), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).border(3.dp, Brand, CircleShape))
            Spacer(Modifier.width(16.dp))
            Text(
                ride.destination ?: "내릴 역 없음",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (ride.destination != null) FontWeight.Bold else FontWeight.Normal,
                color = if (ride.destination != null) MaterialTheme.colorScheme.onSurface else Muted,
                modifier = Modifier.weight(1f),
            )
            if (ride.destination != null) Text("내릴 역", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Brand)
        }
    }
}

@Composable
private fun AlightContent(ride: ActiveRide, onEnd: () -> Unit) {
    StatusBarIcons(dark = false)
    val v = ride.view
    // 역 이름이 작게 시작해 커지며 나타난다
    val grow = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }

    Column(Modifier.fillMaxSize().background(Brand).safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            v.line?.let { LineChip(it, onDark = true) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Ripple()
            Spacer(Modifier.height(18.dp))
            Text("내릴 준비하세요", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text("이번 역", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f))
            Text(
                v.thisStation ?: "",
                fontSize = 76.sp, lineHeight = 88.sp, fontWeight = FontWeight.Black, color = Color.White,
                modifier = Modifier.scale(grow.value),
            )
            Text("내리실 역이에요", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
            v.nextStation?.let {
                Spacer(Modifier.height(20.dp))
                Text("다음 역 $it", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Button(
            onClick = onEnd,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("탑승 끝내기", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

/**
 * 깃발에서 퍼지는 파동. 폰 진동(0.4초씩 0.2초 쉬고 세 번 = 0, 0.6, 1.2초에 시작)과 같은 박자로 세 번 퍼지고,
 * 3초마다 반복한다. 진동은 화면이 바뀌는 순간 같이 울린다(AlightVibrator).
 */
@Composable
private fun Ripple() {
    val t = rememberInfiniteTransition(label = "ripple")
    val rings = listOf(0, 600, 1200).map { start ->
        t.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = RIPPLE_PERIOD
                    0f at start using LinearEasing
                    1f at (start + RIPPLE_SPREAD) using LinearEasing
                    0f at (start + RIPPLE_SPREAD + 1)
                },
            ),
            label = "ring$start",
        )
    }
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val base = 38.dp.toPx()
            for (ring in rings) {
                val p = ring.value
                if (p <= 0f) continue
                drawCircle(
                    Color.White.copy(alpha = 0.45f * (1f - p)),
                    radius = base + (size.minDimension / 2 - base) * p,
                    style = Stroke(3.dp.toPx()),
                )
            }
            drawCircle(Color.White.copy(alpha = 0.18f), radius = base + 12.dp.toPx())
        }
        Box(Modifier.size(76.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = Brand, modifier = Modifier.size(34.dp))
        }
    }
}

private const val RIPPLE_PERIOD = 3000
private const val RIPPLE_SPREAD = 1300

/** 골라 둔 열차를 폰이 확인했는지·놓쳤는지(이번정거장은 고른 열차를 그대로 따라가기만 한다). iOS BannerView와 같은 색. */
@Composable
private fun Banner(ride: ActiveRide) {
    val v = ride.view
    val ok = v.confirmed && v.notice == null
    val warn = v.notice != null
    val fg = if (ok) Color(0xFF14804A) else if (warn) Color(0xFFB45309) else Brand
    val bg = if (ok) Color(0xFFE7F6EE) else if (warn) Color(0xFFFFF4E5) else BrandTint
    Text(
        (if (ok) "✓ " else if (warn) "↻ " else "☝ ") + RideTexts.banner(ride),
        color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().background(bg, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp).testTag("ride-banner"),
    )
}

/** 경로 구간: "태평 ▶ 선릉" "선릉 ▶ 강남". 지금 구간을 칠한다. */
@Composable
private fun LegChips(legs: List<RideLeg>, current: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        legs.forEach { leg ->
            val on = leg.line == current
            val c = lineColor(leg.line)
            Row(
                Modifier.background(if (on) c else Color.White, CircleShape).border(1.5.dp, if (on) Color.Transparent else c, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LineBadge(leg.line, size = 18.dp)
                Spacer(Modifier.width(4.dp))
                Text("${leg.from} ▶ ${leg.to}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Ink)
            }
        }
    }
}

/** 가로 노선도(이번정거장 4708과 같은 배치): 역이 한 줄로 늘어서고 그 위에 열차. 골라 둔 열차는 칠하고, 다른 열차를 누르면 그 열차로. */
@Composable
private fun StripCard(strip: TrainStrip, picking: String?, onPick: (StripTrain) -> Unit) {
    val listState = rememberLazyListState()
    val focus = strip.trains.firstOrNull { it.selected }?.let { strip.stations.indexOf(it.station) }?.takeIf { it >= 0 } ?: strip.boardIndex
    LaunchedEffect(strip.leg, focus) { listState.scrollToItem((focus - 2).coerceAtLeast(0)) }
    val c = lineColor(strip.leg.line)
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp)) {
        Row {
            Text("${strip.leg.from} → ${strip.leg.to} 방향 열차", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            strip.trains.minOfOrNull { it.seenSecondsAgo }?.let { Text("${it}초 전 기준", fontSize = 12.sp, color = Muted) }
        }
        Spacer(Modifier.height(10.dp))
        // 열차가 있는 칸과 없는 칸의 역 줄이 같은 높이에 오게 아래로 맞춘다
        LazyRow(state = listState, verticalAlignment = Alignment.Bottom) {
            itemsIndexed(strip.stations) { i, name ->
                val board = i == strip.boardIndex
                Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(Modifier.height(80.dp), verticalArrangement = Arrangement.Bottom) {
                        strip.trains.filter { it.station == name }.forEach { t ->
                            Column(
                                Modifier.width(66.dp)
                                    .background(if (t.selected) c else Color.White, RoundedCornerShape(10.dp))
                                    .border(1.5.dp, if (t.selected) Color.Transparent else Color(0xFFC9D1E0), RoundedCornerShape(10.dp))
                                    .clickable(enabled = picking == null && !t.selected) { onPick(t) }
                                    .padding(vertical = 5.dp)
                                    .testTag("train-${t.trainNo}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val fg = if (t.selected) Color.White else Ink
                                Text(RideTexts.terminal(t), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
                                if (picking == t.trainNo) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = fg)
                                else Text(t.trainNo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
                                Text("🚆", fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFC9D1E0)))
                        Box(Modifier.size(if (board) 16.dp else 10.dp).background(if (board) c else Color(0xFFC9D1E0), CircleShape))
                    }
                    Text(name, fontSize = 13.sp, fontWeight = if (board) FontWeight.ExtraBold else FontWeight.Normal, color = if (board) c else Ink, maxLines = 1)
                }
            }
        }
    }
}

