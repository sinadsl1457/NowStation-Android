package com.nowstation.app.schedule.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nowstation.app.MainActivity
import com.nowstation.app.R
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.ReservationLiveDisplay
import com.nowstation.app.schedule.domain.TimeText
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 나갈 시각 전 알림: "10분 뒤 출발 예정이에요" + 탑승 시작 / 5분 뒤 다시.
 * 저장하는 순간 오늘 곧 나갈 예약이면 계속 떠 있는 카드(남은 시간 + 탑승 시작 / 예약 취소)도 띄운다(iOS Live Activity와 같은 일).
 */
@Singleton
class ReservationNotifications @Inject constructor(@ApplicationContext private val context: Context) : ReservationLiveDisplay {
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun ensureChannel() {
        // 다른 앱을 보는 중에도 눈에 띄고 소리가 나야 해서 높은 중요도(탑승 중 진행 알림과 채널을 나눈다)
        val channel = NotificationChannel(CHANNEL_ID, "출발 예약 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "예약한 나갈 시각 전에 알려 줘요"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun show(r: Reservation, now: ZonedDateTime) {
        ensureChannel()
        val departure = now.toLocalDate().atTime(r.time).atZone(now.zone).let { if (it.isBefore(now.minusHours(12))) it.plusDays(1) else it }
        // 알람은 정각보다 몇백 ms 늦게 깨어나서 버리면 "9분 뒤"가 된다 → 올림
        val minutes = ((Duration.between(now, departure).seconds + 59) / 60).coerceAtLeast(0)
        val title = if (minutes > 0) "${minutes}분 뒤 출발 예정이에요" else "지금 출발할 시각이에요"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_train)
            .setContentTitle(title)
            .setContentText("${TimeText.of(r.time)} · ${r.origin.name} → ${r.destination.name}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(startRide(r.id, OPEN))
            .addAction(0, "탑승 시작", startRide(r.id, START))
            .addAction(0, "5분 뒤 다시", snooze(r.id))
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId(r.id), notification)
    }

    fun dismiss(id: String) = manager.cancel(notificationId(id))

    override fun showCard(r: Reservation, departure: ZonedDateTime) {
        // 남은 시간은 몇 초마다 바뀌어 조용해야 해서 알림(소리)과 채널을 나눈다
        manager.createNotificationChannel(
            NotificationChannel(CARD_CHANNEL_ID, "출발 예약 카드", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "예약한 나갈 시각까지 남은 시간"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        val until = departure.toInstant().toEpochMilli()
        val route = "${r.origin.name} → ${r.destination.name}"
        val card = NotificationCompat.Builder(context, CARD_CHANNEL_ID)
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
            .setRequestPromotedOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(startRide(r.id, OPEN))
            .addAction(0, "탑승 시작", startRide(r.id, START))
            .addAction(0, if (r.repeats) "닫기" else "예약 취소", closeCard(r.id))
            // 나갈 시각 10분 뒤에는 저절로 내린다
            .setTimeoutAfter(until - System.currentTimeMillis() + CARD_GRACE_MILLIS)
            .build()
        manager.notify(cardId(r.id), card)
    }

    override fun hideCard(id: String) = manager.cancel(cardId(id))

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

    private fun closeCard(id: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReservationReceiver::class.java).setAction(ReservationReceiver.ACTION_CLOSE_CARD).setData(AndroidReservationAlarms.uriOf(id)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notificationId(id: String) = NOTIFICATION_BASE + (id.hashCode() and 0xFFFF)
    private fun cardId(id: String) = CARD_BASE + (id.hashCode() and 0xFFFF)

    companion object {
        const val CHANNEL_ID = "reservation"
        const val START = "com.nowstation.app.action.START_RESERVATION"
        const val OPEN = "com.nowstation.app.action.OPEN_RESERVATION"
        const val CARD_CHANNEL_ID = "reservation_card"
        private const val NOTIFICATION_BASE = 2_000
        private const val CARD_BASE = 200_000
        private const val CARD_GRACE_MILLIS = 10 * 60 * 1000L
    }
}
