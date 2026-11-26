package com.nowstation.app.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.nowstation.app.core.ui.BrandTint
import com.nowstation.app.core.ui.Ink
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

/** 홈의 "예약" 칸. 예약이 없으면 칸째로 숨긴다(처음 쓰는 사람의 홈을 그대로). 시각순 앞의 4개만, 전체 목록은 제목 오른쪽 "전체 N"으로. */
@Composable
fun ReservationList(list: List<Reservation>, today: LocalDate, actions: ReservationRowActions, onShowAll: () -> Unit) {
    if (list.isEmpty()) return
    var openId by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("예약", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348), modifier = Modifier.weight(1f))
            ShowAllButton(list.size, onShowAll)
        }
        list.take(HOME_RESERVATION_LIMIT).forEach { r ->
            key(r.id) { SwipeReservationRow(r, today, actions, open = openId == r.id, onOpenChange = { openId = if (it) r.id else null }) }
        }
    }
}

/** 섹션 제목 오른쪽 "전체 N ›". 글자만 있는 가벼운 버튼이라 눌리는 자리는 넉넉히(48dp 높이). */
@Composable
private fun ShowAllButton(count: Int, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = "예약 전체 보기", onClick = onClick)
            .heightIn(min = 48.dp).padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("전체 $count", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Muted)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
    }
}

/** 홈의 "전체 N"으로 들어오는 전체 예약 목록. 화면 이동 라이브러리 없이 설정 화면처럼 옆에서 밀려 들어온다. */
@Composable
fun ReservationListScreen(list: List<Reservation>, today: LocalDate, actions: ReservationRowActions, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    DarkStatusBarIcons()
    var openId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Ground)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Ink) }
            Text("예약", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
            Text(" ${list.size}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Brand)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("왼쪽으로 밀면 수정하거나 삭제할 수 있어요", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp))
            list.forEach { r ->
                key(r.id) { SwipeReservationRow(r, today, actions, open = openId == r.id, onOpenChange = { openId = if (it) r.id else null }) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val ActionSize = 68.dp
private val ActionGap = 8.dp
/** 버튼 둘 + 사이 간격 + 카드와의 간격. */
private val RevealWidth = ActionSize * 2 + ActionGap * 2
private val Danger = Color(0xFFE5484D)
private val DangerTint = Color(0xFFFDECEC)
private val Settle = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)

/**
 * 왼쪽으로 밀면 카드 옆에서 둥근 수정·삭제 버튼이 미는 만큼 커지며 나타난다. 반 넘게(또는 빠르게) 밀면 열린 채 멈추고 짧게 진동.
 * 한 번에 한 줄만 열린다(다른 줄을 밀기 시작하면 닫힘). 열린 줄을 누르면 닫기만 한다(실수로 시트가 뜨지 않게).
 * 화면 낭독기는 같은 동작을 사용자 지정 동작으로 받는다.
 */
@Composable
private fun SwipeReservationRow(r: Reservation, today: LocalDate, actions: ReservationRowActions, open: Boolean, onOpenChange: (Boolean) -> Unit) {
    val reveal = with(LocalDensity.current) { RevealWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var pastHalf by remember { mutableStateOf(false) }
    LaunchedEffect(open) { offset.animateTo(if (open) -reveal else 0f, Settle) }
    val progress = (-offset.value / reveal).coerceIn(0f, 1f)

    Box(
        Modifier.fillMaxWidth().semantics {
            customActions = listOf(
                CustomAccessibilityAction("수정") { actions.onEdit(r.id); true },
                CustomAccessibilityAction("삭제") { actions.onDelete(r.id); true },
            )
        },
    ) {
        // 밀었을 때만 그린다(닫힌 카드 뒤로 비치지 않게)
        if (progress > 0f) {
            Row(
                Modifier.matchParentSize().graphicsLayer {
                    alpha = progress
                    val scale = 0.6f + 0.4f * progress
                    scaleX = scale; scaleY = scale
                    transformOrigin = TransformOrigin(1f, 0.5f)
                },
                horizontalArrangement = Arrangement.spacedBy(ActionGap, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwipeAction("수정", Icons.Outlined.Edit, tint = Brand, background = BrandTint) {
                    onOpenChange(false)
                    actions.onEdit(r.id)
                }
                SwipeAction("삭제", Icons.Outlined.DeleteOutline, tint = Danger, background = DangerTint) {
                    onOpenChange(false)
                    actions.onDelete(r.id)
                }
            }
        }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { d ->
                        val next = (offset.value + d).coerceIn(-reveal, 0f)
                        scope.launch { offset.snapTo(next) }
                        val half = next < -reveal / 2
                        if (half != pastHalf) {
                            pastHalf = half
                            if (half) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStarted = { if (!open) onOpenChange(true) },
                    onDragStopped = { v ->
                        val keep = offset.value < -reveal / 2 || v < -1500f
                        pastHalf = false
                        // open이 이미 같은 값이면 LaunchedEffect가 다시 돌지 않으니 여기서 직접 맞춘다
                        onOpenChange(keep)
                        offset.animateTo(if (keep) -reveal else 0f, Settle)
                    },
                ),
        ) {
            ReservationRow(
                r, today,
                onEdit = { if (open || offset.value < 0f) onOpenChange(false) else actions.onEdit(r.id) },
                onToggle = { actions.onToggle(r.id, it) },
            )
        }
    }
}

@Composable
private fun SwipeAction(label: String, icon: ImageVector, tint: Color, background: Color, onClick: () -> Unit) {
    Column(
        Modifier.width(ActionSize).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(background).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}
