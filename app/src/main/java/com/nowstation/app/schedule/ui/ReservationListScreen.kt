package com.nowstation.app.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.DarkStatusBarIcons
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.Muted
import com.nowstation.app.schedule.domain.Reservation
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** 홈 목록·전체 목록이 같이 쓰는 줄 동작. */
class ReservationRowActions(
    val onEdit: (String) -> Unit,
    val onToggle: (String, Boolean) -> Unit,
    val onDelete: (String) -> Unit,
)

/** 홈에 보이는 예약 수(개발자 결정: 많으면 홈이 길어져 4개까지, 나머지는 전체 목록에서). */
const val HOME_RESERVATION_LIMIT = 4

/** 홈의 "예약" 칸. 예약이 없으면 칸째로 숨긴다(처음 쓰는 사람의 홈을 그대로). 시각순 앞의 4개만, 넘치면 전체 보기 줄. */
@Composable
fun ReservationList(list: List<Reservation>, today: LocalDate, actions: ReservationRowActions, onShowAll: () -> Unit) {
    if (list.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("예약", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348), modifier = Modifier.padding(horizontal = 4.dp))
        list.take(HOME_RESERVATION_LIMIT).forEach { r -> key(r.id) { SwipeReservationRow(r, today, actions) } }
        if (list.size > HOME_RESERVATION_LIMIT) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .clickable(onClick = onShowAll).heightIn(min = 52.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("예약 ${list.size}개 모두 보기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Brand)
            }
        }
    }
}

/** 홈의 "모두 보기"로 들어오는 전체 예약 목록. 화면 이동 라이브러리 없이 설정 화면처럼 옆에서 밀려 들어온다. */
@Composable
fun ReservationListScreen(list: List<Reservation>, today: LocalDate, actions: ReservationRowActions, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    DarkStatusBarIcons()
    Column(Modifier.fillMaxSize().background(Ground)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color(0xFF141B2D)) }
            Text("예약 ${list.size}개", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF141B2D))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("왼쪽으로 밀면 수정·삭제할 수 있어요", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(horizontal = 4.dp))
            list.forEach { r -> key(r.id) { SwipeReservationRow(r, today, actions) } }
            Box(Modifier.height(16.dp))
        }
    }
}

private val ActionWidth = 76.dp

/**
 * 왼쪽으로 밀면 뒤에서 수정·삭제 버튼이 나온다(iOS 목록처럼). 절반 넘게 밀거나 빠르게 밀면 열린 채 멈춘다.
 * 줄을 누르면 전처럼 고치기 시트, 스위치는 켜고 끄기. 화면 낭독기는 같은 동작을 사용자 지정 동작으로 받는다.
 */
@Composable
private fun SwipeReservationRow(r: Reservation, today: LocalDate, actions: ReservationRowActions) {
    val reveal = with(LocalDensity.current) { (ActionWidth * 2).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(16.dp)
    fun settle(open: Boolean) = scope.launch { offset.animateTo(if (open) -reveal else 0f) }
    Box(
        Modifier.fillMaxWidth().clip(shape).semantics {
            customActions = listOf(
                CustomAccessibilityAction("수정") { actions.onEdit(r.id); true },
                CustomAccessibilityAction("삭제") { actions.onDelete(r.id); true },
            )
        },
    ) {
        // 밀었을 때만 그린다: 둥근 모서리 틈으로 빨간색이 비치지 않게
        if (offset.value < 0f) {
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
                SwipeAction("수정", Brand) { settle(false); actions.onEdit(r.id) }
                SwipeAction("삭제", Color(0xFFD93A3A)) { actions.onDelete(r.id) }
            }
        }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceIn(-reveal, 0f)) } },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { v -> settle(offset.value < -reveal / 2 || v < -1500f) },
                ),
        ) {
            ReservationRow(
                r, today,
                // 열린 줄을 누르면 먼저 닫는다(실수로 시트가 뜨지 않게)
                onEdit = { if (offset.value < 0f) settle(false) else actions.onEdit(r.id) },
                onToggle = { actions.onToggle(r.id, it) },
            )
        }
    }
}

@Composable
private fun SwipeAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.width(ActionWidth).fillMaxHeight().background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White) }
}
