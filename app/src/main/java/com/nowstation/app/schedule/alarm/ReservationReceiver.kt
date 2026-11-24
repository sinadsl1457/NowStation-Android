package com.nowstation.app.schedule.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nowstation.app.schedule.domain.Reservation
import com.nowstation.app.schedule.domain.Reservations
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/** 예약 알람이 울릴 때, "5분 뒤 다시"를 누를 때, 재부팅·시각 변경·권한 변경 뒤(OS가 알람을 지움) 깨어난다. */
@AndroidEntryPoint
class ReservationReceiver : BroadcastReceiver() {
    @Inject lateinit var reservations: Reservations
    @Inject lateinit var notifications: ReservationNotifications
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val id = AndroidReservationAlarms.idOf(intent)
        when (intent.action) {
            ACTION_ALERT -> {
                val r = id?.let(reservations::find) ?: return
                notifications.show(r, now())
                reservations.onAlerted(r.id)
            }
            ACTION_SNOOZE_ALERT -> id?.let(reservations::find)?.let { notifications.show(it, now()) }
            ACTION_CLOSE_CARD -> id?.let(reservations::closeCard)
            ACTION_SNOOZE -> id?.let {
                notifications.dismiss(it)
                reservations.snooze(it)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> reservations.rearmAll()
        }
    }

    private fun now() = ZonedDateTime.now(clock).withZoneSameInstant(Reservation.ZONE)

    companion object {
        const val ACTION_ALERT = "com.nowstation.app.action.RESERVATION_ALERT"
        const val ACTION_SNOOZE_ALERT = "com.nowstation.app.action.RESERVATION_SNOOZE_ALERT"
        const val ACTION_SNOOZE = "com.nowstation.app.action.RESERVATION_SNOOZE"
        const val ACTION_CLOSE_CARD = "com.nowstation.app.action.RESERVATION_CLOSE_CARD"
    }
}
