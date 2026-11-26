package com.nowstation.app.core.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** 홈 위에 덮는 밝은 바탕 화면(설정·전체 예약)에서 상태 표시줄 글자를 어둡게. 닫으면 홈(파란 머리)의 흰 글자로 되돌린다. */
@Composable
fun DarkStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        controller.isAppearanceLightStatusBars = true
        onDispose { controller.isAppearanceLightStatusBars = false }
    }
}
