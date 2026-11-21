package com.nowstation.app.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowstation.app.BuildConfig
import com.nowstation.app.core.ui.Brand
import com.nowstation.app.core.ui.Ground
import com.nowstation.app.core.ui.Muted

private val Ink = Color(0xFF141B2D)
private val Divider = Color(0xFFEEF1F6)
private val Chevron = Color(0xFF8A93A6)

private enum class Page { Main, Contact, Privacy, Licenses }

/**
 * 설정. 모두 앱 안에서: 알림 스위치, 사용법 다시 보기, 문의하기(서버로 보냄), 개인정보처리방침(웹뷰), 오픈소스 라이선스, 출처, 버전.
 * Play 정책상 위치처럼 민감한 정보를 다루면 개인정보처리방침을 앱 안에서도 볼 수 있어야 한다.
 */
@Composable
fun SettingsScreen(alertSettings: AlertSettings, onBack: () -> Unit, onShowGuide: () -> Unit) {
    var page by remember { mutableStateOf(Page.Main) }
    DarkStatusBarIcons()
    when (page) {
        Page.Main -> MainPage(alertSettings, onBack, onShowGuide, open = { page = it })
        Page.Contact -> ContactPage(onBack = { page = Page.Main })
        Page.Privacy -> PrivacyPage(onBack = { page = Page.Main }, onContact = { page = Page.Contact })
        Page.Licenses -> LicensesPage(onBack = { page = Page.Main })
    }
}

@Composable
private fun MainPage(alertSettings: AlertSettings, onBack: () -> Unit, onShowGuide: () -> Unit, open: (Page) -> Unit) {
    BackHandler(onBack = onBack)
    val prefs by alertSettings.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Ground)) {
        TopBar("설정", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Section("알림") {
                    NotificationPermissionRow()
                    Toggle("갈아타기 알림", "갈아탈 역 한 정거장 전에 화면 위로 알려 줘요", prefs.transfer) { on -> alertSettings.update { it.copy(transfer = on) } }
                    Line()
                    Toggle("내릴 역 알림", "내릴 역 한 정거장 전에 화면 위로 알려 줘요", prefs.alight) { on -> alertSettings.update { it.copy(alight = on) } }
                    Line()
                    Toggle("진동", "위 알림 때 진동(무음 모드에서도 울려요)", prefs.vibrate, enabled = prefs.transfer || prefs.alight) { on -> alertSettings.update { it.copy(vibrate = on) } }
                }
                Text(
                    "탑승 중 잠금화면·알림창의 현재역 표시는 화면을 꺼도 탑승을 따라가는 데 필요해서 끌 수 없어요. 탑승을 끝내면 사라져요.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = Muted, modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Section("도움말") {
                Item("사용법 다시 보기", onClick = onShowGuide)
                Line()
                Item("문의하기", "궁금한 점, 오류, 탑승 기록 삭제 요청") { open(Page.Contact) }
            }
            Section("정보") {
                Item("개인정보처리방침") { open(Page.Privacy) }
                Line()
                Item("오픈소스 라이선스") { open(Page.Licenses) }
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

/** 알림 권한이 꺼져 있으면 그 자리에서 허용을 묻는다. 두 번 거절해 OS가 더 묻지 않을 때만 시스템 화면으로(앱이 대신 켤 수 없음). */
@Composable
private fun NotificationPermissionRow() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasNotificationPermission()) }
    var asked by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) granted = context.hasNotificationPermission() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; asked++ }
    if (granted) return
    val activity = context as Activity
    Column(Modifier.fillMaxWidth().background(Color(0xFFFFF4DC)).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("알림이 꺼져 있어요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("잠금화면 현재역과 갈아타기·내릴 역 알림을 받으려면 알림을 허용해 주세요.", fontSize = 13.sp, color = Color(0xFF5A4A1E))
        Button(
            onClick = {
                val canAsk = asked == 0 || activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                if (canAsk) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
        ) { Text("알림 허용", fontWeight = FontWeight.Bold) }
    }
    Line()
}

private fun android.content.Context.hasNotificationPermission() =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun ContactPage(onBack: () -> Unit, vm: ContactViewModel = hiltViewModel()) {
    BackHandler(onBack = onBack)
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Ground).imePadding()) {
        TopBar("문의하기", onBack)
        if (s.sent) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Brand, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("보냈어요", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(if (s.contact.isNotBlank()) "적어 주신 이메일로 답장 드릴게요." else "보내 주셔서 고마워요.", fontSize = 15.sp, color = Muted)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBack) { Text("설정으로", fontWeight = FontWeight.Bold, color = Brand) }
            }
            return@Column
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = s.message, onValueChange = vm::onMessage,
                label = { Text("내용") },
                placeholder = { Text("궁금한 점이나 불편한 점을 적어 주세요. 탑승 기록 삭제를 원하면 탑승한 날짜와 시각을 적어 주세요.") },
                supportingText = { Text("${s.message.length} / ${ContactViewModel.MAX_MESSAGE}") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
            )
            OutlinedTextField(
                value = s.contact, onValueChange = vm::onContact,
                label = { Text("답장받을 이메일(선택)") },
                singleLine = true,
                isError = s.contactInvalid,
                supportingText = { Text(if (s.contactInvalid) "이메일 모양이 아니에요" else "적지 않으면 답장 없이 확인만 해요") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("앱 버전(${BuildConfig.VERSION_NAME})과 기기 종류가 함께 가요. 문의는 1년 뒤 지워요.", fontSize = 12.sp, color = Muted)
            s.error?.let { Text(it, fontSize = 14.sp, color = Color(0xFFD32F2F)) }
            Button(
                onClick = vm::send, enabled = s.canSend,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand),
            ) {
                if (s.sending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("보내기", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 개인정보처리방침: 서버의 /privacy를 앱 안 웹뷰로. 메일 링크는 앱 안 문의하기로 돌린다. */
@Composable
private fun PrivacyPage(onBack: () -> Unit, onContact: () -> Unit) {
    var failed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var web by remember { mutableStateOf<WebView?>(null) }
    BackHandler { if (web?.canGoBack() == true) web?.goBack() else onBack() }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TopBar("개인정보처리방침", onBack)
        Box(Modifier.fillMaxSize().navigationBarsPadding()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (request.url.scheme == "mailto") {
                                    onContact()
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) {
                                    failed = true
                                    loading = false
                                }
                            }
                        }
                        web = this
                    }
                },
                update = { view ->
                    if (view.tag != reload) {
                        view.tag = reload
                        failed = false
                        loading = true
                        view.loadUrl(BuildConfig.API_BASE_URL + "privacy")
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading) CircularProgressIndicator(color = Brand, modifier = Modifier.align(Alignment.Center))
            if (failed) {
                Column(Modifier.fillMaxSize().background(Color.White).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("불러오지 못했어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("인터넷 연결을 확인하고 다시 시도해 주세요.", fontSize = 14.sp, color = Muted)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { reload++ }) { Text("다시 시도", fontWeight = FontWeight.Bold, color = Brand) }
                }
            }
        }
    }
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
private fun LicensesPage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
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
            Section("Apache License 2.0") {
                Text(APACHE_NOTICE, fontSize = 12.sp, lineHeight = 18.sp, color = Ink, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

/** 라이선스 고지(앱 밖 링크 대신 앱 안에 싣는다). */
private const val APACHE_NOTICE = """Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."""

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
private fun Item(title: String, sub: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (sub != null) Text(sub, fontSize = 13.sp, color = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Chevron, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Toggle(title: String, sub: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (enabled) Ink else Chevron)
            Text(sub, fontSize = 13.sp, color = Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked && enabled, onCheckedChange = onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Brand))
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
