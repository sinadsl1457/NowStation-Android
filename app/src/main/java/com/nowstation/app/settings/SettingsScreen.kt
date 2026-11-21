package com.nowstation.app.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowstation.app.BuildConfig
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.Muted

private val Ink = Color(0xFF141B2D)
private val Divider = Color(0xFFEEF1F6)
private val Chevron = Color(0xFF8A93A6)

/** 스토어 등록정보·개인정보처리방침의 연락처와 같은 주소(삭제 요청도 이리로). */
private const val CONTACT_EMAIL = "sinadsl1457@gmail.com"

/**
 * 설정: 알림(시스템 설정으로), 사용법 다시 보기, 문의, 개인정보처리방침, 오픈소스 라이선스, 출처, 버전.
 * Play 정책상 위치처럼 민감한 정보를 다루면 개인정보처리방침을 앱 안에서도 열 수 있어야 한다.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit, onShowGuide: () -> Unit) {
    var licenses by remember { mutableStateOf(false) }
    if (licenses) {
        LicensesScreen(onBack = { licenses = false })
        return
    }
    BackHandler(onBack = onBack)
    DarkStatusBarIcons()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Ground)) {
        TopBar("설정", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section("알림") {
                // 알림 켜고 끄기·소리는 시스템 화면에 맡긴다(앱 안 스위치를 따로 두면 채널 설정과 어긋난다)
                Item("알림 설정", "진행 중 알림·내릴 역 알림 켜고 끄기, 소리·진동", Icons.AutoMirrored.Filled.OpenInNew) {
                    context.open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            }
            Section("도움말") {
                Item("사용법 다시 보기", onClick = onShowGuide)
                Line()
                Item("문의하기", "메일로 보내요. 탑승 기록 삭제 요청도 여기로", Icons.AutoMirrored.Filled.OpenInNew) {
                    val body = "\n\n---\n앱 버전 ${BuildConfig.VERSION_NAME}\n기기 ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
                    val uri = Uri.parse("mailto:$CONTACT_EMAIL?subject=${Uri.encode("[지금역 문의]")}&body=${Uri.encode(body)}")
                    context.open(Intent(Intent.ACTION_SENDTO, uri))
                }
            }
            Section("정보") {
                // 방침 페이지는 서버와 같은 주소에 있다(주소를 코드에 따로 적지 않게)
                Item("개인정보처리방침", trailing = Icons.AutoMirrored.Filled.OpenInNew) {
                    context.open(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.API_BASE_URL + "privacy")))
                }
                Line()
                Item("오픈소스 라이선스") { licenses = true }
                Line()
                Info("열차 위치 정보", "서울특별시 열린데이터광장 실시간 지하철 정보")
                Line()
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("버전", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(BuildConfig.VERSION_NAME, fontSize = 15.sp, color = Muted)
                }
            }
            Text(
                "열차 정보는 실제보다 30초~1분 늦을 수 있어요. 안내는 참고용이니 역 안내 방송도 함께 확인해 주세요.",
                fontSize = 12.sp, lineHeight = 19.sp, color = Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** 밝은 바탕이라 상태 표시줄 글자를 어둡게. 닫으면 홈(파란 머리)의 흰 글자로 되돌린다. */
@Composable
private fun DarkStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        controller.isAppearanceLightStatusBars = true
        onDispose { controller.isAppearanceLightStatusBars = false }
    }
}

private fun Context.open(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "열 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Ink) }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(horizontal = 4.dp))
        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))) { content() }
    }
}

@Composable
private fun Item(title: String, sub: String? = null, trailing: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (sub != null) Text(sub, fontSize = 13.sp, color = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Icon(trailing, contentDescription = null, tint = Chevron, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Info(title: String, sub: String) {
    Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(sub, fontSize = 13.sp, color = Muted)
    }
}

@Composable
private fun Line() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
}

/** 앱에 들어간 라이브러리(app/build.gradle.kts 의존성)와 라이선스. 모두 Apache License 2.0. */
private val libraries = listOf(
    "AndroidX (Core, Activity, Lifecycle, Hilt ViewModel)" to "Google · Apache License 2.0",
    "Jetpack Compose (UI, Material 3, Material Icons)" to "Google · Apache License 2.0",
    "Dagger Hilt" to "Google · Apache License 2.0",
    "Kotlin Coroutines" to "JetBrains · Apache License 2.0",
    "Kotlin Serialization" to "JetBrains · Apache License 2.0",
    "Retrofit" to "Square · Apache License 2.0",
    "OkHttp (Logging Interceptor)" to "Square · Apache License 2.0",
)

@Composable
private fun LicensesScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Ground)) {
        TopBar("오픈소스 라이선스", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))) {
                libraries.forEachIndexed { i, (name, license) ->
                    if (i > 0) Line()
                    Info(name, license)
                }
            }
            Text(
                "Licensed under the Apache License, Version 2.0. 이 라이선스에 따라 위 라이브러리를 고치지 않고 사용합니다.",
                fontSize = 13.sp, lineHeight = 20.sp, color = Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Section("라이선스 전문") {
                Item("Apache License 2.0", "apache.org", Icons.AutoMirrored.Filled.OpenInNew) {
                    context.open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apache.org/licenses/LICENSE-2.0")))
                }
            }
        }
    }
}
