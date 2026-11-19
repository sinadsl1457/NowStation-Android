package com.nowstation.app.ride.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nowstation.app.MainActivity
import com.nowstation.app.R
import com.nowstation.app.ride.domain.ActiveRide
import com.nowstation.app.ride.ui.RideTexts
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 탑승 중 잠금화면에 남는 진행 중 알림. 갱신마다 소리·진동이 나지 않게 한 번만 알린다.
 * Android 16+는 Live Update로 올려 달라고 요청한다(상태 표시줄 칩 + 잠금화면 맨 위). 사용자가 끄거나
 * 이전 버전이면 시스템이 그냥 일반 진행 중 알림으로 보여 주므로 따로 나누지 않는다.
 */
@Singleton
class RideNotifications @Inject constructor(@ApplicationContext private val context: Context) {

    fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "탑승 중 안내", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "지하철 현재역·다음역"
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        // 진행 중 알림은 몇 초마다 바뀌어 조용해야 하고, 내릴 역 알림은 다른 앱을 보는 중에도 눈에 띄어야 해서 채널을 나눈다.
        // 진동은 AlightVibrator가 알람 용도로 직접 낸다(무음 모드에서도 울리게) → 채널은 팝업만.
        val alight = NotificationChannel(ALIGHT_CHANNEL_ID, "내릴 역 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "내릴 역 직전에 화면 위로 알려 줘요"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, alight))
    }

    /** 내릴 역 직전 한 번: 다른 앱 위로 팝업, 잠금화면에도. 2분 뒤 저절로 사라진다(진행 중 알림이 계속 보여 주므로). */
    fun buildAlight(ride: ActiveRide): Notification =
        NotificationCompat.Builder(context, ALIGHT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_train)
            .setContentTitle(RideTexts.title(ride))
            .setContentText(RideTexts.remaining(ride) ?: RideTexts.detail(ride))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setTimeoutAfter(ALIGHT_TIMEOUT_MILLIS)
            .build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    fun build(ride: ActiveRide?): Notification {
        val open = openApp()
        val end = PendingIntent.getService(
            context, 1,
            Intent(context, RideService::class.java).setAction(RideService.ACTION_END),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val title = ride?.let(RideTexts::title) ?: "탑승을 시작하는 중"
        val detail = ride?.let(RideTexts::detail)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_train)
            .setContentTitle(title)
            .setContentText(detail)
            // Live Update 조건: 제목 있음, 진행 중, 색칠 안 함, 허용된 스타일(BigText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(ride?.let(RideTexts::chip))
            .setSubText(ride?.view?.line)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(0, "종료", end)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "ride"
        const val NOTIFICATION_ID = 1
        const val ALIGHT_CHANNEL_ID = "alight"
        const val ALIGHT_NOTIFICATION_ID = 2
        private const val ALIGHT_TIMEOUT_MILLIS = 2 * 60 * 1000L
    }
}
