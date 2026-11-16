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
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(ride: ActiveRide?): Notification {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
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
    }
}
