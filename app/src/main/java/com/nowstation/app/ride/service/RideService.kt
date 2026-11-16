package com.nowstation.app.ride.service

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.domain.RideTracker
import com.nowstation.app.ride.motion.MotionDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 탑승 중에만 도는 포그라운드 서비스. 앱을 닫아도 몇 초마다 서버에서 받아 와 잠금화면 알림을 갱신한다.
 * 서버가 실시간 API를 15초마다 받으니 그보다 자주 받는다(앱↔서버는 하루 한도와 무관).
 * 가속도 센서로 열차 출발·도착을 감지해 보내고, 내릴 역 알림이 켜지면 진동 + 팝업 알림을 한 번 띄운다.
 * 화면이 꺼져도 센서가 멈추지 않게 탑승 중에는 CPU를 깨워 둔다(최대 [WAKE_LOCK_MILLIS]).
 */
@AndroidEntryPoint
class RideService : LifecycleService() {
    @Inject lateinit var tracker: RideTracker
    @Inject lateinit var notifications: RideNotifications
    @Inject lateinit var accelerometer: Accelerometer
    @Inject lateinit var vibrator: AlightVibrator

    private var loops: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        ServiceCompat.startForeground(
            this,
            RideNotifications.NOTIFICATION_ID,
            notifications.build(tracker.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NowStation:ride")
            .apply { acquire(WAKE_LOCK_MILLIS) }
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_END) {
            lifecycleScope.launch { tracker.end() }
            return START_NOT_STICKY
        }
        if (loops == null) {
            loops = lifecycleScope.launch {
                launch {
                    while (isActive) {
                        tracker.refresh()
                        delay(REFRESH_MILLIS)
                    }
                }
                launch {
                    val detector = MotionDetector()
                    accelerometer.samples()
                        .mapNotNull(detector::accept)
                        .flowOn(Dispatchers.Default)
                        .collect { motion ->
                            Log.d(TAG, "움직임 ${motion.kind} ${motion.at}")
                            tracker.reportMotion(motion)
                        }
                }
                var previous: ActiveRide? = null
                tracker.state.collect { ride ->
                    val manager = getSystemService(NotificationManager::class.java)
                    if (ride != null && ActiveRide.alightAlertStarted(previous, ride)) {
                        vibrator.vibrate()
                        manager.notify(RideNotifications.ALIGHT_NOTIFICATION_ID, notifications.buildAlight(ride))
                    }
                    previous = ride
                    if (ride == null) {
                        manager.cancel(RideNotifications.ALIGHT_NOTIFICATION_ID)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        manager.notify(RideNotifications.NOTIFICATION_ID, notifications.build(ride))
                    }
                }
            }
        }
        // 시스템이 서비스를 죽였다가 다시 살리면 탑승 상태(메모리)는 이미 없으니 다시 살리지 않는다
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_END = "com.nowstation.app.ride.END"
        private const val REFRESH_MILLIS = 5_000L
        private const val WAKE_LOCK_MILLIS = 3 * 60 * 60 * 1000L
        private const val TAG = "RideService"
    }
}
