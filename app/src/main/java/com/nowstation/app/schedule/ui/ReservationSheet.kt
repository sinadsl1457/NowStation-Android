package com.nowstation.app.schedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.LineDot
import com.nowstation.app.core.ui.Muted
import com.nowstation.app.schedule.domain.RepeatLabel
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.TimeText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ReservationSheetActions(
    val onClose: () -> Unit,
    val onStep: (Int) -> Unit,
    val onOpenDial: (Boolean) -> Unit,
    val onSetTime: (LocalTime) -> Unit,
    val onLead: (Int) -> Unit,
    val onPage: (EditorPage) -> Unit,
    val onToggleDay: (DayOfWeek) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
)

/** 예약 만들기·고치기. 반복은 iOS 알람처럼 "반복" 줄 → 요일 목록(시트 안에서 화면이 바뀜). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationSheet(state: EditorState, today: LocalDate, actions: ReservationSheetActions) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = actions.onClose, sheetState = sheet, containerColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(),
        ) {
            when (state.page) {
                EditorPage.MAIN -> MainPage(state, today, actions)
                EditorPage.REPEAT -> {
                    BackHandler { actions.onPage(EditorPage.MAIN) }
                    RepeatPage(state, actions)
                }
            }
        }
    }
    if (state.dialOpen) {
        TimeDialDialog(initial = state.time, onDismiss = { actions.onOpenDial(false) }, onConfirm = actions.onSetTime)
    }
}

@Composable
private fun MainPage(state: EditorState, today: LocalDate, actions: ReservationSheetActions) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (state.id == null) "출발 예약" else "예약 고치기", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${state.origin.name} → ${state.destination.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                (state.origin.lines + state.destination.lines).distinct().forEach { LineDot(it) }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("나갈 시각")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepButton("−5분") { actions.onStep(-Reservation.STEP_MINUTES) }
                Box(
                    Modifier.weight(1f).height(52.dp).background(Ground, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClickLabel = "나갈 시각 고르기") { actions.onOpenDial(true) },
                    contentAlignment = Alignment.Center,
                ) { Text(TimeText.of(state.time), fontSize = 30.sp, fontWeight = FontWeight.Black) }
                StepButton("+5분") { actions.onStep(Reservation.STEP_MINUTES) }
            }
        }

        Column(Modifier.background(Ground, RoundedCornerShape(14.dp))) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { actions.onPage(EditorPage.REPEAT) }
                    .padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("반복", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(RepeatLabel.of(state.days), fontSize = 15.sp, color = Muted)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF8A93A6))
            }
            // 한 번짜리는 오늘만(개발자 결정: 내일은 고르지 않음). 오늘 칩은 무엇이 정해졌는지 보여 주려고 남긴다
            if (state.days.isEmpty()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("날짜", fontSize = 14.sp, color = Muted, modifier = Modifier.weight(1f))
                    Chip("오늘(${RepeatLabel.short(today.dayOfWeek)})", selected = true, onClick = null)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("미리 알림")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Reservation.LEADS.forEach { n -> Chip("${n}분 전", selected = state.leadMinutes == n) { actions.onLead(n) } }
            }
        }

        val blocked = !state.canSave
        Text(
            ReservationTexts.summary(state.time, state.days, state.leadMinutes, state.passed),
            fontSize = 14.sp, lineHeight = 21.sp,
            color = if (blocked) MaterialTheme.colorScheme.error else Color(0xFF2A3348),
            modifier = Modifier.fillMaxWidth().background(Ground, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        )

        Column {
            Button(
                onClick = actions.onSave,
                enabled = !blocked,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text("저장", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            if (state.id != null) {
                TextButton(onClick = actions.onDelete, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("예약 삭제", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RepeatPage(state: EditorState, actions: ReservationSheetActions) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
            IconButton(onClick = { actions.onPage(EditorPage.MAIN) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text("반복", fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.background(Ground, RoundedCornerShape(14.dp))) {
            RepeatLabel.ORDER.forEachIndexed { i, day ->
                val on = day in state.days
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.Checkbox, onClickLabel = if (on) "빼기" else "넣기") { actions.onToggleDay(day) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("매주 ${RepeatLabel.short(day)}요일", fontSize = 16.sp, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Filled.Check, contentDescription = "고름", tint = Brand, modifier = Modifier.size(22.dp))
                }
                if (i < RepeatLabel.ORDER.lastIndex) HorizontalDivider(color = Color(0xFFE6EAF1), modifier = Modifier.padding(start = 16.dp))
            }
        }
        Text(
            "요일을 고르면 오늘부터 끌 때까지 그 요일마다 알려요. 하나도 고르지 않으면 한 번만 알려요.",
            fontSize = 13.sp, lineHeight = 20.sp, color = Muted, modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun Label(text: String) = Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted)

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).border(1.5.dp, Color(0xFFD5DCE8), RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348)) }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: (() -> Unit)?) {
    Box(
        Modifier.height(40.dp)
            .background(if (selected) Brand else Color.White, RoundedCornerShape(20.dp))
            .border(1.5.dp, if (selected) Brand else Color(0xFFD5DCE8), RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else Color(0xFF2A3348)) }
}

/** 홈 화면의 예약 목록 한 줄. */
@Composable
fun ReservationRow(r: Reservation, today: LocalDate, onEdit: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable(onClickLabel = "예약 고치기", onClick = onEdit).padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val dim = if (r.enabled) 1f else 0.5f
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ReservationTexts.dayLabel(r, today), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = (if (r.repeats) Muted else Brand).copy(alpha = dim))
                Text(TimeText.of(r.time).substringAfter(' '), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF141B2D).copy(alpha = dim))
                Text(TimeText.of(r.time).substringBefore(' '), fontSize = 11.sp, color = Muted.copy(alpha = dim))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${r.origin.name} → ${r.destination.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF141B2D).copy(alpha = dim))
                Text(ReservationTexts.detail(r), fontSize = 13.sp, color = Muted.copy(alpha = dim))
            }
        }
        Switch(
            checked = r.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

/** 홈의 "예약" 칸. 예약이 없으면 칸째로 숨긴다(처음 쓰는 사람의 홈을 그대로). */
@Composable
fun ReservationList(list: List<Reservation>, today: LocalDate, onEdit: (String) -> Unit, onToggle: (String, Boolean) -> Unit) {
    if (list.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("예약", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2A3348), modifier = Modifier.padding(horizontal = 4.dp))
        list.forEach { r -> ReservationRow(r, today, onEdit = { onEdit(r.id) }, onToggle = { onToggle(r.id, it) }) }
    }
}
