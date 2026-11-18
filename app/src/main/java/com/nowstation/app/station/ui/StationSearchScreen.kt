package com.nowstation.app.station.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.BrandTint
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.LineChip
import com.nowstation.app.core.ui.Muted
import com.nowstation.app.core.ui.StatusBarIcons
import com.nowstation.app.station.domain.Station

/** 역 검색. 홈의 칸을 누르면 열리고, 고르거나 뒤로 가면 닫힌다. */
@Composable
fun StationSearchScreen(state: HomeUiState, actions: HomeActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val query = state.query.trim()
    StatusBarIcons(dark = true)

    Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 12.dp)) {
            IconButton(onClick = actions.onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text(if (state.target == PickTarget.ORIGIN) "타는 역" else "내릴 역", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand) },
            trailingIcon = if (state.query.isNotEmpty()) {
                { IconButton(onClick = { actions.onQueryChange("") }) { Icon(Icons.Filled.Cancel, contentDescription = "지우기", tint = Color(0xFFC9D1E0)) } }
            } else null,
            placeholder = { Text("역 이름") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Brand,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color(0xFFF1F4FA),
                unfocusedContainerColor = Color(0xFFF1F4FA),
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).focusRequester(focus),
        )
        if (query.isNotEmpty()) {
            Text("검색 결과 ${state.results.size}", style = MaterialTheme.typography.labelLarge, color = Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            items(state.results, key = { it.name }) { station ->
                val chosen = station.name == (if (state.target == PickTarget.ORIGIN) state.origin else state.destination)?.name
                StationResult(station, query, chosen, onClick = { actions.onPick(station) })
            }
        }
        if (state.lines.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp).fillMaxWidth().background(Ground, RoundedCornerShape(14.dp)).padding(14.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("${state.lines.joinToString(" · ")} 역을 찾을 수 있어요", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@Composable
private fun StationResult(station: Station, query: String, chosen: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (chosen) BrandTint else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            Modifier.size(40.dp).background(if (chosen) Brand else Color(0xFFF1F4FA), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = if (chosen) Color.White else Muted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(highlight(station.name, query), fontSize = 18.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { station.lines.forEach { LineChip(it) } }
    }
}

/** 입력한 글자를 파랗게. */
private fun highlight(name: String, query: String): AnnotatedString = buildAnnotatedString {
    val at = if (query.isEmpty()) -1 else name.indexOf(query)
    if (at < 0) {
        append(name)
        return@buildAnnotatedString
    }
    append(name.substring(0, at))
    withStyle(SpanStyle(color = Brand, fontWeight = FontWeight.Black)) { append(name.substring(at, at + query.length)) }
    append(name.substring(at + query.length))
}
