package com.nowstation.app.schedule.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nowstation.app.MainActivity
import com.nowstation.app.R
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.TimeText
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 나갈 시각 전 알림을 계속 떠 있는 카드로(개발자 결정: 10분 전부터 상태 표시줄·잠금화면에 남은 시간이 보이게).
 * 울릴 때 한 번 소리·팝업, 그 뒤엔 시스템이 나갈 시각까지 거꾸로 센다. 탑승 시작 / 5분 뒤 다시(5분 넘게 남았을 때만) / 닫기.
 */
@Singleton
class ReservationNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun ensureChannel() {
        // 다른 앱을 보는 중에도 눈에 띄고 소리가 나야 해서 높은 중요도(탑승 중 진행 알림과 채널을 나눈다)
        val channel = NotificationChannel(CHANNEL_ID, "출발 예약 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "예약한 나갈 시각 전에 알려 줘요"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * 카드를 띄운다. [refresh]면 이미 떠 있는 카드를 버튼만 바꿔 조용히 다시 그린다(5분 밑으로 내려간 순간).
     * 닫거나 탑승을 시작해 카드가 없으면 되살리지 않는다.
     */
    fun show(r: Reservation, now: ZonedDateTime, refresh: Boolean = false) {
        ensureChannel()
        if (refresh && manager.activeNotifications.none { it.id == notificationId(r.id) }) return
        val departure = now.toLocalDate().atTime(r.time).atZone(now.zone).let { if (it.isBefore(now.minusHours(12))) it.plusDays(1) else it }
        val snoozable = !refresh && Reservation.canSnooze(departure, now)
        val until = departure.toInstant().toEpochMilli()
        val route = "${r.origin.name} → ${r.destination.name}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_train)
            .setContentTitle("${TimeText.of(r.time)} 출발")
            .setContentText("$route · 출발까지")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$route · 출발까지"))
            // 시스템이 나갈 시각까지 거꾸로 센다(앱이 깨어 있지 않아도)
            .setWhen(until)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Android 16+: 상태 표시줄 칩·잠금화면 맨 위(탑승 중 알림과 같은 Live Update)
            .setRequestPromotedOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(startRide(r.id, OPEN))
            .addAction(0, "탑승 시작", startRide(r.id, START))
            .apply { if (snoozable) addAction(0, "5분 뒤 다시", snooze(r.id)) }
            .addAction(0, "닫기", close(r.id))
            // 나갈 시각 10분 뒤에는 저절로 내린다
            .setTimeoutAfter(Duration.between(now, departure).toMillis().coerceAtLeast(0) + GRACE_MILLIS)
            .build()
        manager.notify(notificationId(r.id), notification)
        // 남은 시간이 5분 밑으로 내려가는 순간 "5분 뒤 다시"를 뺀 카드로 바꿔 끼운다
        if (snoozable) refreshAt(r.id, departure.minusMinutes(Reservation.SNOOZE_MINUTES))
    }

    fun dismiss(id: String) {
        manager.cancel(notificationId(id))
        alarmManager.cancel(refreshIntent(id))
    }

    private fun refreshAt(id: String, at: ZonedDateTime) {
        val millis = at.toInstant().toEpochMilli()
        // 화면을 바꾸는 일이라 몇 초 늦어도 된다: 권한이 있으면 제시각, 없으면 대략
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, refreshIntent(id))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, refreshIntent(id))
        }
    }

    private fun refreshIntent(id: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReservationReceiver::class.java).setAction(ReservationReceiver.ACTION_REFRESH_CARD).setData(AndroidReservationAlarms.uriOf(id)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** 알림을 누르면 앱만 열고, "탑승 시작"을 누르면 그 경로로 바로 탑승을 시작한다(앱이 앞에 있어야 서비스를 띄울 수 있다). */
    private fun startRide(id: String, mode: String): PendingIntent = PendingIntent.getActivity(
        context, mode.hashCode(),
        Intent(context, MainActivity::class.java)
            .setAction(mode)
            .setData(AndroidReservationAlarms.uriOf(id))
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun snooze(id: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReservationReceiver::class.java).setAction(ReservationReceiver.ACTION_SNOOZE).setData(AndroidReservationAlarms.uriOf(id)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun close(id: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReservationReceiver::class.java).setAction(ReservationReceiver.ACTION_CLOSE).setData(AndroidReservationAlarms.uriOf(id)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notificationId(id: String) = NOTIFICATION_BASE + (id.hashCode() and 0xFFFF)

    companion object {
        const val CHANNEL_ID = "reservation"
        const val START = "com.nowstation.app.action.START_RESERVATION"
        const val OPEN = "com.nowstation.app.action.OPEN_RESERVATION"
        private const val NOTIFICATION_BASE = 2_000
        private const val GRACE_MILLIS = 10 * 60 * 1000L
    }
}
