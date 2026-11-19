package com.nowstation.app.ride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.BrandTint
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.Ink
import com.nowstation.app.core.ui.LineBadge
import com.nowstation.app.core.ui.Muted
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.TrainChoice

data class TrainPickActions(
    val pick: (TrainChoice) -> Unit,
    val unsure: () -> Unit,
    val notYet: () -> Unit,
    val dismissPickFailed: () -> Unit,
)

/** 탑승 시작 직후 "지금 탄 열차"를 고른다(걷는 중·늦게 켬에서 자동 판단이 실패: 2026-11-19 실기기). iOS와 같은 배치·문구. */
@Composable
fun TrainPickScreen(ride: ActiveRide, state: TrainPickUiState, actions: TrainPickActions) {
    Column(
        Modifier.fillMaxSize().background(Ground).safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("지금 타고 있는 열차를 골라 주세요", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("${ride.origin} → ${ride.destination.orEmpty()}", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Box(Modifier.weight(1f)) {
            when {
                !state.loaded -> Row(
                    Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("  열차를 찾는 중…", color = Muted)
                }
                state.trains.isEmpty() -> Text(
                    if (state.failed) "열차 정보를 받지 못했어요. 잠시 후 다시 받아 와요." else "지금 이 구간에 보이는 열차가 없어요.",
                    color = Muted, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${RideTexts.seenAgo(state.trains.minOf { it.seenSecondsAgo })} · 실제보다 한두 정거장 늦게 보일 수 있어요",
                        color = Muted, fontSize = 12.sp,
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.trains, key = { "${it.line}-${it.trainNo}" }) { train ->
                            TrainRow(train, busy = state.picking == train, enabled = state.picking == null) { actions.pick(train) }
                        }
                    }
                }
            }
        }

        Button(
            onClick = actions.unsure,
            enabled = state.picking == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandTint, contentColor = Brand),
        ) { Text("목록에 없어요 / 모르겠어요", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        TextButton(onClick = actions.notYet, enabled = state.picking == null, modifier = Modifier.fillMaxWidth()) {
            Text("아직 안 탔어요", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (state.pickFailed) {
        AlertDialog(
            onDismissRequest = actions.dismissPickFailed,
            title = { Text("열차를 고르지 못했어요") },
            text = { Text("방금 목록에서 사라진 열차일 수 있어요. 다시 골라 주세요.") },
            confirmButton = { TextButton(onClick = actions.dismissPickFailed) { Text("확인") } },
        )
    }
}

@Composable
private fun TrainRow(train: TrainChoice, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
            .testTag("train-${train.trainNo}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineBadge(train.line, size = 30.dp)
        Column(Modifier.weight(1f)) {
            Text(RideTexts.trainPlace(train), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            RideTexts.trainTerminal(train)?.let { Text(it, color = Muted, fontSize = 14.sp) }
        }
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted)
        Spacer(Modifier.size(0.dp))
    }
}
