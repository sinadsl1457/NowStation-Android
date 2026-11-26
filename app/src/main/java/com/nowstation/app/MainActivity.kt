package com.nowstation.app

import android.Manifest
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import com.nowstation.app.schedule.alarm.ReservationNotifications
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.Reservations
import com.nowstation.app.schedule.ui.ReservationList
import com.nowstation.app.schedule.ui.ReservationListScreen
import com.nowstation.app.schedule.ui.ReservationRowActions
import com.nowstation.app.schedule.ui.ReservationSheet
import com.nowstation.app.schedule.ui.ReservationSheetActions
import com.nowstation.app.schedule.ui.ReservationViewModel
import com.nowstation.app.schedule.ui.SaveOutcome
import java.time.Clock
import java.time.ZonedDateTime
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import com.nowstation.app.core.ui.LaunchIntro
import com.nowstation.app.guide.GuideScreen
import com.nowstation.app.guide.GuideStore
import com.nowstation.app.settings.SettingsScreen
import com.nowstation.app.settings.AlertSettings
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.nowstation.app.core.ui.LaunchOverlay
import com.nowstation.app.core.ui.LocalLaunchIntro
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowstation.app.core.ui.NowStationTheme
import com.nowstation.app.ride.domain.RideNotice
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.ui.RideScreen
import com.nowstation.app.ride.ui.RideViewModel
import com.nowstation.app.station.ui.HomeActions
import com.nowstation.app.station.ui.HomeScreen
import com.nowstation.app.station.ui.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** 화면이 둘뿐이라 내비게이션 없이 "탑승 중이면 탑승 화면, 아니면 홈"으로 고른다. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var tracker: RideTracker
    @Inject lateinit var guide: GuideStore
    @Inject lateinit var alertSettings: AlertSettings
    @Inject lateinit var reservations: Reservations
    @Inject lateinit var reservationNotifications: ReservationNotifications
    @Inject lateinit var clock: Clock

    /** 예약 알림에서 들어온 부탁(탑승 시작·열기). 화면이 처리하면 비운다. */
    private val reservationIntent = mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data != null) reservationIntent.value = intent
    }

    /** 잠금화면 진행 중 알림에 필요. 거부해도 앱 화면으로는 쓸 수 있다. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    private companion object {
        /** iOS 예약 시트의 안내와 같은 문구. */
        const val NOTIFICATION_OFF = "예약한 시각에 알려 드리려면 알림을 허용해 주세요"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템 시작 화면이 서서히 사라지면 아이콘이 잠깐 비었다가 다시 나타난다 → 바로 걷어 내고 같은 자리의 앱 그림(LaunchIntro)이 이어 그린다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        enableEdgeToEdge()
        // 처음 실행이면 가이드 마지막 장 버튼에서 묻는다(이유를 보여 준 뒤). 이미 본 사람은 전처럼 켤 때
        if (guide.seen) askNotificationPermission()
        if (savedInstanceState == null && intent?.data != null) reservationIntent.value = intent
        setContent {
            NowStationTheme {
                val ride by tracker.state.collectAsStateWithLifecycle()
                // 첫 실행 애니메이션은 앱을 새로 켤 때 홈에서만(회전 등으로 다시 만들어질 때·탑승 중엔 없음)
                var intro by remember { mutableStateOf(if (savedInstanceState == null) LaunchIntro() else null) }
                val scope = rememberCoroutineScope()
                var showGuide by remember { mutableStateOf(!guide.seen) }
                var showSettings by remember { mutableStateOf(false) }
                var showAllReservations by remember { mutableStateOf(false) }
                LaunchedEffect(intro) {
                    val running = intro ?: return@LaunchedEffect
                    try {
                        withFrameNanos { } // 첫 화면이 그려진 뒤 시작(시작 직후 버벅임에 시간을 빼앗기지 않게)
                        running.time.animateTo(LaunchIntro.TOTAL, tween((LaunchIntro.TOTAL * 1000).toInt(), easing = LinearEasing))
                    } finally {
                        intro = null
                    }
                }
                LaunchedEffect(Unit) {
                    tracker.notices.collect { notice ->
                        when (notice) {
                            RideNotice.Lost -> Toast.makeText(this@MainActivity, "서버에서 탑승 정보가 사라져 끝냈어요", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val current = ride
                val home: HomeViewModel = hiltViewModel()
                val reserve: ReservationViewModel = hiltViewModel()
                val reservationList by reserve.list.collectAsStateWithLifecycle()
                val editor by reserve.editor.collectAsStateWithLifecycle()
                val today = remember(reservationList) { ZonedDateTime.now(clock).withZoneSameInstant(Reservation.ZONE).toLocalDate() }
                // 저장 순서: 알림 허용 → "알람 및 리마인더"(개발자 결정). 허용하고 돌아오면 다시 저장해 보고, 그래도 꺼져 있으면 저장하지 않고 알린다
                val exactAlarmSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    when (reserve.save()) {
                        SaveOutcome.NeedsNotification -> toast(NOTIFICATION_OFF)
                        SaveOutcome.NeedsExactAlarm -> toast("알람 및 리마인더를 허용해야 제시각에 알려 드릴 수 있어요")
                        else -> Unit
                    }
                }
                val askExactAlarm = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        toast("제시각에 알리려면 알람 및 리마인더를 허용해 주세요")
                        exactAlarmSettings.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                    }
                }
                val afterNotificationAsk = {
                    when (reserve.save()) {
                        SaveOutcome.NeedsNotification -> toast(NOTIFICATION_OFF)
                        SaveOutcome.NeedsExactAlarm -> askExactAlarm()
                        else -> Unit
                    }
                }
                val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { afterNotificationAsk() }
                val notificationSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { afterNotificationAsk() }
                // 가이드에서 한 번 거절했으면 OS가 한 번 더 묻게 해 준다. 두 번 거절했거나 앱 알림·예약 채널을 끈 경우엔 앱이 켤 수 없어 알림 설정 화면으로
                val askNotification = {
                    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                    if (canAsk) {
                        notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        toast(NOTIFICATION_OFF)
                        notificationSettings.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                    }
                }
                val pending by reservationIntent
                LaunchedEffect(pending) {
                    val i = pending ?: return@LaunchedEffect
                    reservationIntent.value = null
                    val id = i.data?.lastPathSegment ?: return@LaunchedEffect
                    reservationNotifications.dismiss(id)
                    val r = reservations.find(id)
                    if (i.action == ReservationNotifications.START && r != null) {
                        if (tracker.state.value != null) {
                            Toast.makeText(this@MainActivity, "이미 탑승 중이에요", Toast.LENGTH_SHORT).show()
                        } else {
                            intro = null
                            home.startRoute(r.origin, r.destination)
                        }
                    }
                }
                if (current != null) {
                    LaunchedEffect(Unit) { intro = null }
                    // 탑승마다 새로(끝난 탑승의 노선도를 계속 받지 않게)
                    val vm: RideViewModel = hiltViewModel(key = current.view.rideId)
                    val strip by vm.strip.collectAsStateWithLifecycle()
                    val picking by vm.picking.collectAsStateWithLifecycle()
                    RideScreen(current, strip, picking, onPick = vm::pick, onEnd = vm::end)
                } else {
                    val vm = home
                    val state by vm.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.message) {
                        state.message?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                            vm.dismissMessage()
                        }
                    }
                    val actions = remember(vm) {
                        HomeActions(
                            vm::onQueryChange, vm::pick, vm::edit, vm::swap, vm::start, vm::useRecent, vm::closeSearch,
                            onShowGuide = { showGuide = true }, onShowSettings = { showSettings = true },
                            onReserve = {
                                val s = vm.state.value
                                if (s.origin != null && s.destination != null) reserve.create(s.origin, s.destination)
                            },
                        )
                    }
                    val running = intro
                    CompositionLocalProvider(LocalLaunchIntro provides running) {
                        Box {
                            val rowActions = remember(reserve) { ReservationRowActions(onEdit = reserve::edit, onToggle = reserve::setEnabled, onDelete = reserve::remove) }
                            HomeScreen(state, actions) {
                                ReservationList(reservationList, today, rowActions, onShowAll = { showAllReservations = true })
                            }
                            // 다 지우면 빈 화면에 남지 않게 홈으로
                            LaunchedEffect(reservationList.isEmpty()) { if (reservationList.isEmpty()) showAllReservations = false }
                            AnimatedVisibility(
                                visible = showAllReservations,
                                enter = slideInHorizontally { it },
                                exit = slideOutHorizontally { it },
                            ) {
                                ReservationListScreen(reservationList, today, rowActions, onBack = { showAllReservations = false })
                            }
                            editor?.let { e ->
                                ReservationSheet(
                                    e, today,
                                    ReservationSheetActions(
                                        onClose = reserve::close, onStep = reserve::step, onOpenDial = reserve::openDial,
                                        onSetTime = reserve::setTime, onLead = reserve::setLead, onPage = reserve::showPage,
                                        onToggleDay = reserve::toggleDay,
                                        onSave = {
                                            when (reserve.save()) {
                                                SaveOutcome.NeedsNotification -> askNotification()
                                                SaveOutcome.NeedsExactAlarm -> askExactAlarm()
                                                else -> Unit
                                            }
                                        },
                                        onDelete = reserve::delete,
                                    ),
                                )
                            }
                            AnimatedVisibility(
                                visible = showSettings,
                                enter = slideInHorizontally { it },
                                exit = slideOutHorizontally { it },
                            ) {
                                SettingsScreen(alertSettings, onBack = { showSettings = false }, onShowGuide = { showGuide = true })
                            }
                            // 런치 애니메이션이 끝나 홈이 된 뒤 위로 올라온다. 처음 한 번만(다시 보기는 설정에서)
                            AnimatedVisibility(
                                visible = running == null && showGuide,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it } + fadeOut(),
                            ) {
                                GuideScreen(onDone = {
                                    guide.markSeen()
                                    showGuide = false
                                    askNotificationPermission()
                                })
                            }
                            if (running != null) {
                                LaunchOverlay(running, onSkip = { scope.launch { running.time.snapTo(LaunchIntro.TOTAL) } })
                            }
                        }
                    }
                }
            }
        }
    }
}
