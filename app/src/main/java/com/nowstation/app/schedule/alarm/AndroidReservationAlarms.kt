package com.nowstation.app.schedule.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.nowstation.app.MainActivity
import com.nowstation.app.schedule.domain.ReservationAlarms
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * 정확한 시각은 setAlarmClock(상태 표시줄에 알람 아이콘, 절전 중에도 제시각). Android 12+는 "알람 및 리마인더" 권한이
 * 있어야 해서 예약 저장 전에 받는다(개발자 결정). 나중에 사용자가 권한을 끄면 대략 그 시각에라도 울리게 낮춰 건다.
 */
class AndroidReservationAlarms @Inject constructor(@ApplicationContext private val context: Context) : ReservationAlarms {
    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun canExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    /** 앱 알림 전체(Android 13+는 알림 권한)와 예약 알림 채널 둘 다 켜져 있어야 카드가 보인다. 채널은 첫 알림 전엔 없을 수 있다. */
    override fun canNotify(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(ReservationNotifications.CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    override fun set(id: String, at: ZonedDateTime) = arm(fire(id, ReservationReceiver.ACTION_ALERT), at)

    override fun snooze(id: String, at: ZonedDateTime) = arm(fire(id, ReservationReceiver.ACTION_SNOOZE_ALERT), at)

    override fun cancel(id: String) {
        manager.cancel(fire(id, ReservationReceiver.ACTION_ALERT))
        manager.cancel(fire(id, ReservationReceiver.ACTION_SNOOZE_ALERT))
    }

    private fun arm(operation: PendingIntent, at: ZonedDateTime) {
        val millis = at.toInstant().toEpochMilli()
        if (canExact()) {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(millis, openApp()), operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }

    /** 예약마다 다른 주소(data)로 구분한다: 같은 예약의 알림·5분 뒤 다시는 action으로 나뉜다. */
    private fun fire(id: String, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReservationReceiver::class.java).setAction(action).setData(uriOf(id)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** 상태 표시줄 알람 아이콘을 길게 누르면 열리는 화면. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        fun uriOf(id: String): Uri = Uri.parse("nowstation://reservation/$id")
        fun idOf(intent: Intent): String? = intent.data?.lastPathSegment
    }
}
