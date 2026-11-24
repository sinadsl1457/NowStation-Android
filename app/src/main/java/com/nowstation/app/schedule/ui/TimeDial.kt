package com.nowstation.app.schedule.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.Ink
import com.nowstation.app.core.ui.Muted
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

private val RingGrey = Color(0xFFD5DCE8)
private val Face = Color(0xFFEAEFF7)

/** 왼쪽 위에서 빛을 받은 공(입체감). */
private fun Modifier.lit(vararg stops: Pair<Float, Color>) = drawBehind {
    drawCircle(Brush.radialGradient(*stops, center = Offset(size.width * 0.35f, size.height * 0.28f), radius = size.width * 0.8f))
}

/**
 * 나갈 시각 고르기: 입체 순환선 시계판(개발자가 3안 중 고름). 기본 시계판과 같은 "시 → 분" 순서지만
 * 숫자 둘레를 순환선으로, 고른 칸을 열차(파란 공)로 그린다. 5분 단위만 받는다(개발자 결정: 1분 조정 없음).
 */
@Composable
fun TimeDialDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute / 5 * 5) }
    var pickingHour by remember { mutableStateOf(true) }
    val pm = hour >= 12

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFF7F9FD)) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("나갈 시각", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted)
                Dial(
                    hour = hour, minute = minute, pickingHour = pickingHour,
                    onPick = { k ->
                        if (pickingHour) {
                            hour = k + if (pm) 12 else 0
                            pickingHour = false
                        } else {
                            minute = k * 5
                        }
                    },
                    onSelectHour = { pickingHour = true },
                    onSelectMinute = { pickingHour = false },
                )
                AmPmSwitch(pm = pm, onChange = { toPm -> if (toPm != pm) hour = (hour + 12) % 24 })
                Text(
                    if (pickingHour) "시를 고르면 분으로 넘어가요" else "가운데 시를 누르면 다시 고칠 수 있어요",
                    fontSize = 13.sp, color = Muted,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("확인", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun Dial(hour: Int, minute: Int, pickingHour: Boolean, onPick: (Int) -> Unit, onSelectHour: () -> Unit, onSelectMinute: () -> Unit) {
    val selected = if (pickingHour) hour % 12 else if (minute % 5 == 0) minute / 5 else -1
    val sweep = if (pickingHour) (hour % 12) * 30f else minute * 6f
    Box(
        Modifier
            .size(280.dp)
            .shadow(18.dp, CircleShape, ambientColor = Brand.copy(alpha = 0.25f), spotColor = Brand.copy(alpha = 0.25f))
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F2))), CircleShape),
    ) {
        // 안쪽 판은 살짝 파인 것처럼: 가장자리가 어둡게
        Box(
            Modifier.offset(14.dp, 14.dp).size(252.dp)
                .background(Brush.radialGradient(0.80f to Face, 1f to Color(0xFFD9E0EC)), CircleShape),
        )
        // 순환선: 12시 방향에서 고른 칸까지 지나온 구간만 파랗게
        Canvas(Modifier.offset(41.dp, 41.dp).size(198.dp)) {
            val w = 6.dp.toPx()
            val inset = w / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(RingGrey, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w))
            if (sweep > 0f) drawArc(Brand, -90f, sweep, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w))
        }
        for (k in 0 until 12) {
            val angle = Math.toRadians(k * 30.0)
            val x = 140 + 96 * sin(angle) - 22
            val y = 140 - 96 * cos(angle) - 22
            val label = if (pickingHour) (if (k == 0) 12 else k).toString() else "%02d".format(k * 5)
            Slot(
                label = label,
                selected = k == selected,
                description = if (pickingHour) "${label}시" else "${label}분",
                modifier = Modifier.offset(x.dp, y.dp),
                onClick = { onPick(k) },
            )
        }
        Hub(hour, minute, pickingHour, onSelectHour, onSelectMinute, Modifier.offset(85.dp, 85.dp))
    }
}

@Composable
private fun Slot(label: String, selected: Boolean, description: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(44.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            // 고른 칸 = 열차: 빛 받은 파란 공
            Box(
                Modifier.size(40.dp)
                    .shadow(8.dp, CircleShape, ambientColor = Brand, spotColor = Brand)
                    .lit(0f to Color(0xFF7C9BF5), 0.45f to Color(0xFF2F5DE0), 1f to Color(0xFF1238A8)),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        } else {
            Box(
                Modifier.size(32.dp)
                    .shadow(3.dp, CircleShape)
                    .lit(0f to Color.White, 0.6f to Color(0xFFF1F4F9), 1f to Color(0xFFDCE2EC)),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = Color(0xFF2A3348), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** 가운데 노브: 고른 시각, 시·분을 눌러 그쪽을 다시 고른다. */
@Composable
private fun Hub(hour: Int, minute: Int, pickingHour: Boolean, onSelectHour: () -> Unit, onSelectMinute: () -> Unit, modifier: Modifier) {
    Column(
        modifier.size(110.dp)
            .shadow(12.dp, CircleShape)
            .lit(0f to Color.White, 0.55f to Color(0xFFEEF2F8), 1f to Color(0xFFCDD5E3)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (hour < 12) "오전" else "오후", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            Text(
                "$h12", fontSize = 30.sp, fontWeight = FontWeight.Black, color = if (pickingHour) Brand else Ink,
                modifier = Modifier.clickable(role = Role.Button, onClick = onSelectHour).semantics { contentDescription = "시 고르기" },
            )
            Text(":", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF8A93A6))
            Text(
                "%02d".format(minute), fontSize = 30.sp, fontWeight = FontWeight.Black, color = if (pickingHour) Ink else Brand,
                modifier = Modifier.clickable(role = Role.Button, onClick = onSelectMinute).semantics { contentDescription = "분 고르기" },
            )
        }
    }
}

/** 파인 홈 안에서 고른 쪽만 솟아오르는 오전/오후. */
@Composable
private fun AmPmSwitch(pm: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.background(Color(0xFFE6EBF3), RoundedCornerShape(16.dp)).padding(4.dp),
    ) {
        listOf(false to "오전", true to "오후").forEach { (value, text) ->
            val on = pm == value
            Box(
                Modifier.width(88.dp).padding(0.dp)
                    .then(if (on) Modifier.shadow(3.dp, RoundedCornerShape(12.dp)).background(Color.White, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable(role = Role.RadioButton) { onChange(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (on) Brand else Muted) }
        }
    }
}
