package com.nowstation.app.ride.service

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nowstation.app.ride.domain.RideTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 탑승 중에만 도는 포그라운드 서비스. 앱을 닫아도 몇 초마다 서버에서 받아 와 잠금화면 알림을 갱신한다.
 * 서버가 실시간 API를 15초마다 받으니 그보다 자주 받는다(앱↔서버는 하루 한도와 무관).
 */
@AndroidEntryPoint
class RideService : LifecycleService() {
    @Inject lateinit var tracker: RideTracker
    @Inject lateinit var notifications: RideNotifications

    private var loops: Job? = null

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        ServiceCompat.startForeground(
            this,
            RideNotifications.NOTIFICATION_ID,
            notifications.build(tracker.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
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
                tracker.state.collect { ride ->
                    if (ride == null) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        getSystemService(NotificationManager::class.java)
                            .notify(RideNotifications.NOTIFICATION_ID, notifications.build(ride))
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
    }
}
