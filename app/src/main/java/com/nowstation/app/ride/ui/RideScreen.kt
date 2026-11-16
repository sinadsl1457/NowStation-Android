package com.nowstation.app.ride.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideStatus
import com.nowstation.app.station.ui.LineChip

/** 탑승 중: 이번 역을 아주 크게(흔들리는 열차에서 한눈에), 다음 역과 상태는 그 아래. */
@Composable
fun RideScreen(ride: ActiveRide, onEnd: () -> Unit) {
    val v = ride.view
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) {
            v.line?.let { LineChip(it) }
            Spacer(Modifier.height(24.dp))
            val waiting = v.status == RideStatus.WAITING
            Text(if (waiting) "기다리는 역" else "이번 역", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                v.thisStation ?: "확인 중",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = if (v.prepareToAlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(RideTexts.detail(ride), style = MaterialTheme.typography.titleMedium)
            if (v.prepareToAlight) {
                Spacer(Modifier.height(16.dp))
                Text("내릴 준비하세요", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text(
                listOfNotNull("타는 역 ${ride.origin}", ride.destination?.let { "내릴 역 $it" }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("탑승 끝내기") }
        }
    }
}
