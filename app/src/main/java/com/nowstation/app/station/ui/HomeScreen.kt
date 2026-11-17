package com.nowstation.app.station.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nowstation.app.core.ui.lineColor
import com.nowstation.app.station.domain.Station

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onPick: (Station) -> Unit,
    onEdit: (PickTarget) -> Unit,
    onClearDestination: () -> Unit,
    onStart: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("지금역", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "타는 역을 고르고 시작하면, 탄 열차를 찾아 잠금화면에 현재역을 보여 줘요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            PickRow("타는 역", state.origin, selected = state.target == PickTarget.ORIGIN, onClick = { onEdit(PickTarget.ORIGIN) })
            Spacer(Modifier.height(8.dp))
            PickRow(
                "내릴 역(선택)", state.destination, selected = state.target == PickTarget.DESTINATION,
                onClick = { onEdit(PickTarget.DESTINATION) },
                onClear = if (state.destination != null) onClearDestination else null,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(if (state.target == PickTarget.ORIGIN) "타는 역 검색" else "내릴 역 검색") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(state.results, key = { it.name }) { station ->
                    StationRow(station, onClick = { onPick(station) })
                    HorizontalDivider()
                }
            }
            Button(
                onClick = onStart,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 4.dp),
            ) { Text(if (state.isStarting) "시작하는 중…" else "시작", style = MaterialTheme.typography.titleMedium) }
            // 공공데이터 이용 조건: 출처 표시
            Text(
                "열차 위치: 서울특별시 열린데이터광장 실시간 지하철 정보",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PickRow(label: String, station: Station?, selected: Boolean, onClick: () -> Unit, onClear: (() -> Unit)? = null) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(88.dp))
            Text(station?.name ?: "고르기", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                color = if (station == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
            if (onClear != null) IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "내릴 역 지우기", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StationRow(station: Station, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    ) {
        Text(station.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            station.lines.forEach { LineChip(it) }
        }
    }
}

@Composable
fun LineChip(line: String) {
    Text(
        line,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier.background(lineColor(line), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
