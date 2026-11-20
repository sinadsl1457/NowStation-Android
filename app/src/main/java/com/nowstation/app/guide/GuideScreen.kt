package com.nowstation.app.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.Hairline
import com.nowstation.app.core.ui.Muted
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

private class GuidePage(val title: String, val body: String)

private val pages = listOf(
    GuidePage("타는 역·내릴 역만 고르세요", "갈아타는 길은 앱이 정해요. 태평에서 강남이면 선릉에서 2호선으로 갈아타요."),
    GuidePage("탈 열차를 눌러요", "승강장 전광판처럼 내 역으로 들어오는 열차가 보여요. 다음 열차는 미리 골라 두고, 다른 열차를 탔다면 눌러서 바꿔요."),
    GuidePage("탔으면 폰은 넣어 두세요", "잠금화면과 알림창에 이번 역과 남은 정거장이 계속 바뀌어요. 다른 앱을 써도 괜찮아요."),
    GuidePage("한 정거장 전에 깨워 드려요", "갈아탈 역과 내릴 역 한 정거장 전에 진동과 알림이 와요. 무음 모드에서도요."),
)

private val SuinBundang = Color(0xFFF5A200)
private val Line2 = Color(0xFF00A84D)
private val Ink = Color(0xFF141B2D)
private val Body = Color(0xFF3A4458)

/**
 * 처음 실행 가이드 4장. 옆으로 넘기면 위 그림이 글자보다 느리게 따라온다.
 * 마지막 장 버튼([onDone])에서 알림 권한을 묻는다(앱을 켜자마자 묻지 않게).
 */
@Composable
fun GuideScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val time by rememberSeconds()
    val last = pager.currentPage == pages.lastIndex
    // 뒤로 가기는 앱을 닫지 않고 앞 장으로, 첫 장이면 건너뛰기와 같게
    BackHandler { if (pager.currentPage > 0) scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } else onDone() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Ground)) {
        val artHeight = maxHeight * 0.56f
        Box(Modifier.fillMaxWidth().height(artHeight).background(Brand)) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.12f), 130.dp.toPx(), Offset(size.width - 70.dp.toPx(), 170.dp.toPx()), style = Stroke(2.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.10f), 100.dp.toPx(), Offset(40.dp.toPx(), size.height - 20.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            // 0이면 이 장이 가운데. 그림만 더 밀어 글자보다 느리게 움직이게
            val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
            // 그림이 글자보다 느리게 움직여 옆 장으로 비어져 나온다 → 장마다 잘라 낸다
            Column(Modifier.fillMaxSize().clipToBounds()) {
                Box(
                    Modifier.fillMaxWidth().height(artHeight).statusBarsPadding().padding(top = 24.dp)
                        .graphicsLayer { translationX = offset * size.width * 0.35f },
                    contentAlignment = Alignment.Center,
                ) {
                    when (page) {
                        0 -> RouteArt(time)
                        1 -> BoardArt(time)
                        2 -> LockArt(time)
                        else -> AlertArt(time)
                    }
                }
                Column(Modifier.padding(horizontal = 28.dp).padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(pages[page].title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text(pages[page].body, fontSize = 16.sp, lineHeight = 25.sp, color = Body)
                }
            }
        }
        if (!last) {
            TextButton(onClick = onDone, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 8.dp, top = 4.dp)) {
                Text("건너뛰기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 28.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val position = pager.currentPage + pager.currentPageOffsetFraction
                repeat(pages.size) { i ->
                    val near = (1 - abs(position - i)).coerceIn(0f, 1f)
                    Box(Modifier.height(8.dp).width(8.dp + 18.dp * near).background(if (near > 0.5f) Brand else Color(0xFFC9D1E0), RoundedCornerShape(4.dp)))
                }
            }
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
            ) {
                Text(if (last) "알림 허용하고 시작하기" else "다음", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                if (last) "알림을 허용해야 잠금화면 표시와 진동 알림을 받아요" else "",
                fontSize = 12.sp, color = Muted, modifier = Modifier.fillMaxWidth().heightIn(min = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** 그림을 움직이는 시계(초). 가이드가 떠 있는 동안만 돈다. */
@Composable
private fun rememberSeconds(): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { seconds.floatValue = (it - start) / 1e9f }
    }
    return seconds
}

private fun clamp(x: Float) = x.coerceIn(0f, 1f)
private fun ease(x: Float) = 1 - (1 - x).pow(3)

private fun Modifier.card(radius: Dp = 22.dp) =
    shadow(16.dp, RoundedCornerShape(radius), ambientColor = Color(0x400A1E5A), spotColor = Color(0x400A1E5A))
        .background(Color.White, RoundedCornerShape(radius))

/** 1장: 역 두 칸이 채워지고, 앱이 정한 길(수인분당 → 선릉 → 2호선)이 그려진다. */
@Composable
private fun RouteArt(time: Float) {
    val c = time % 3.2f
    val yellow = ease(clamp((c - 0.6f) / 0.9f))
    val green = ease(clamp((c - 1.4f) / 0.6f))
    Column(Modifier.width(326.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.fillMaxWidth().card().padding(horizontal = 18.dp, vertical = 8.dp)) {
            Field("타는 역", "태평", filled = true, alpha = clamp(c / 0.3f))
            Box(Modifier.padding(start = 24.dp).fillMaxWidth().height(1.dp).background(Color(0xFFEEF1F6)))
            Field("내릴 역", "강남", filled = false, alpha = clamp((c - 0.3f) / 0.3f))
        }
        Box(Modifier.fillMaxWidth().height(130.dp).card()) {
            Text("앱이 정한 길", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 18.dp, top = 14.dp))
            Text(
                "2호선 갈아타기", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00813B),
                modifier = Modifier.offset(x = 118.dp, y = 34.dp).alpha(clamp((c - 1.3f) / 0.3f))
                    .background(Color(0xFFE6F6EC), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Canvas(Modifier.fillMaxWidth().height(80.dp).offset(y = 50.dp)) {
                val y = 19.dp.toPx()
                val a = 31.dp.toPx()
                val m = 164.dp.toPx()
                val b = 297.dp.toPx()
                val w = 6.dp.toPx()
                drawLine(Color(0xFFEEF1F6), Offset(a, y), Offset(b, y), w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                if (yellow > 0) drawLine(SuinBundang, Offset(a, y), Offset(lerp(a, m, yellow), y), w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                if (green > 0) drawLine(Line2, Offset(m, y), Offset(lerp(m, b, green), y), w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                fun stop(x: Float, ring: Color) {
                    drawCircle(Color.White, 9.dp.toPx(), Offset(x, y))
                    drawCircle(ring, 7.dp.toPx(), Offset(x, y), style = Stroke(4.dp.toPx()))
                }
                stop(a, Brand)
                stop(m, if (yellow >= 1f) Line2 else Color(0xFFC9D1E0))
                stop(b, if (green >= 1f) Line2 else Color(0xFFC9D1E0))
            }
            Row(Modifier.fillMaxWidth().offset(y = 88.dp).padding(horizontal = 11.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("태평", "선릉", "강남").forEach { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, filled: Boolean, alpha: Float) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(12.dp).then(
                if (filled) Modifier.background(Brand, CircleShape) else Modifier.border(3.dp, Brand, CircleShape),
            ),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Muted)
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.alpha(alpha))
        }
    }
}

/** 2장: 열차 점이 야탑 → 모란 → 태평으로 내려오고, 첫 줄을 누르면 골라진다. */
@Composable
private fun BoardArt(time: Float) {
    val c = time % 3.6f
    val picked = c > 2.2f
    val tap = clamp((c - 2.2f) / 0.5f)
    Box(Modifier.width(326.dp).height(316.dp).card()) {
        Text("태평으로 들어오는 열차", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 18.dp, top = 14.dp))
        Canvas(Modifier.width(100.dp).height(316.dp)) {
            val x = 72.dp.toPx()
            drawLine(SuinBundang, Offset(x, 52.dp.toPx()), Offset(x, 272.dp.toPx()), 4.dp.toPx())
            listOf(62f, 162f).forEach { drawCircle(Color.White, 6.dp.toPx(), Offset(x, it.dp.toPx())); drawCircle(SuinBundang, 4.5.dp.toPx(), Offset(x, it.dp.toPx()), style = Stroke(3.dp.toPx())) }
            drawCircle(Color.White, 9.dp.toPx(), Offset(x, 262.dp.toPx()))
            drawCircle(Brand, 7.5.dp.toPx(), Offset(x, 262.dp.toPx()), style = Stroke(3.dp.toPx()))
            fun train(y: Float, color: Color) {
                drawRoundRect(Color.White, Offset(x - 10.dp.toPx(), y - 10.dp.toPx()), androidx.compose.ui.geometry.Size(20.dp.toPx(), 20.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                drawRoundRect(color, Offset(x - 8.dp.toPx(), y - 8.dp.toPx()), androidx.compose.ui.geometry.Size(16.dp.toPx(), 16.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            }
            train((62 + 200 * ease(clamp(c / 2.2f))).dp.toPx(), SuinBundang)
            train((0 + 70 * ease(clamp(c / 3.6f))).dp.toPx(), Color(0xFFC9D1E0))
        }
        Text("야탑", fontSize = 13.sp, color = Muted, modifier = Modifier.offset(x = 10.dp, y = 52.dp).width(50.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("모란", fontSize = 13.sp, color = Muted, modifier = Modifier.offset(x = 10.dp, y = 152.dp).width(50.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("태평", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Brand, modifier = Modifier.offset(x = 10.dp, y = 252.dp).width(50.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Column(Modifier.offset(x = 104.dp, y = 60.dp).width(206.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (picked) SuinBundang else Color.White)
                    .border(2.dp, if (picked) SuinBundang else Hairline, RoundedCornerShape(14.dp)),
            ) {
                TrainRow("8009", "태평 진입 · 지금 타세요", if (picked) Color.White else Ink)
                if (tap > 0f && tap < 1f) {
                    Canvas(Modifier.matchParentSize()) { drawCircle(Color.White.copy(alpha = 0.45f * (1 - tap)), 120.dp.toPx() * tap / 2, Offset(160.dp.toPx(), 30.dp.toPx())) }
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 60.dp).background(Color.White, RoundedCornerShape(14.dp)).border(2.dp, Hairline, RoundedCornerShape(14.dp))) {
                TrainRow("8010", "[2]번째 전역 (야탑) · 약 4분", Ink, sub = Muted)
            }
        }
    }
}

@Composable
private fun TrainRow(no: String, status: String, color: Color, sub: Color = color.copy(alpha = 0.85f)) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(no, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color)
            Spacer(Modifier.width(5.dp))
            Text("왕십리행", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Text(status, fontSize = 12.sp, color = sub)
    }
}

/** 3장: 잠금화면 진행 중 알림과 상태 표시줄의 역 이름이 바뀐다(Android에서 실제로 뜨는 모습). */
@Composable
private fun LockArt(time: Float) {
    val names = listOf("가천대", "복정", "수서", "대모산입구")
    val k = floor((time % 6f) / 1.5f).toInt()
    val fade = clamp(((time % 1.5f) / 1.5f) / 0.15f)
    Box(Modifier.width(240.dp).height(350.dp).shadow(16.dp, RoundedCornerShape(36.dp)).background(Color(0xFF0E1630), RoundedCornerShape(36.dp))) {
        Row(Modifier.padding(start = 18.dp, top = 14.dp).background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(SuinBundang, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(names[k], fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.alpha(fade))
        }
        Text("18:12", fontSize = 46.sp, fontWeight = FontWeight.Light, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp))
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 150.dp).padding(horizontal = 12.dp).fillMaxWidth()
                .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(Brand, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("지금역 · 수인분당선", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
            }
            Text("이번 역 ${names[k]}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.alpha(fade))
            Text("강남까지 ${10 - k}정거장", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348))
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).background(Hairline, RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(0.09f * (k + 1)).height(4.dp).background(Brand, RoundedCornerShape(2.dp)))
            }
        }
    }
}

/** 4장: 갈아탈 역 한 정거장 전 알림이 내려오며 폰이 떤다. */
@Composable
private fun AlertArt(time: Float) {
    val c = time % 3f
    val drop = ease(clamp((c - 0.3f) / 0.4f))
    val shaking = c > 0.7f && c < 1.5f
    Box(Modifier.width(330.dp).height(350.dp), contentAlignment = Alignment.Center) {
        if (shaking) {
            Canvas(Modifier.matchParentSize()) {
                val s = Stroke(3.dp.toPx())
                drawArc(Color.White.copy(alpha = 0.7f), 150f, 60f, false, Offset(8.dp.toPx(), 140.dp.toPx()), androidx.compose.ui.geometry.Size(30.dp.toPx(), 70.dp.toPx()), style = s)
                drawArc(Color.White.copy(alpha = 0.7f), -30f, 60f, false, Offset(size.width - 38.dp.toPx(), 140.dp.toPx()), androidx.compose.ui.geometry.Size(30.dp.toPx(), 70.dp.toPx()), style = s)
            }
        }
        Box(
            Modifier.width(240.dp).height(350.dp)
                .graphicsLayer { translationX = if (shaking) sin(c * 70) * 3.dp.toPx() else 0f }
                .shadow(16.dp, RoundedCornerShape(36.dp)).background(Color.White, RoundedCornerShape(36.dp)).clip(RoundedCornerShape(36.dp)),
        ) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 70.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth(0.6f).height(14.dp).background(Color(0xFFEEF1F6), RoundedCornerShape(7.dp)))
                Box(Modifier.fillMaxWidth().height(90.dp).background(Color(0xFFEEF1F6), RoundedCornerShape(14.dp)))
                Box(Modifier.fillMaxWidth(0.8f).height(14.dp).background(Color(0xFFEEF1F6), RoundedCornerShape(7.dp)))
                Box(Modifier.fillMaxWidth(0.5f).height(14.dp).background(Color(0xFFEEF1F6), RoundedCornerShape(7.dp)))
            }
            Column(
                Modifier.offset(y = lerp(-70f, 12f, drop).dp).padding(horizontal = 10.dp).fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp)).background(Color(0xFFFFF4DC), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFF5D58A), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text("지금역 · 지금", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A5A00))
                Text("다음 역 선릉\n2호선으로 갈아타세요", fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            }
        }
    }
}
