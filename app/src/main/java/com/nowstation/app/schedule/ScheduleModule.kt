package com.nowstation.app.schedule

import com.nowstation.app.schedule.alarm.AndroidReservationAlarms
import com.nowstation.app.schedule.alarm.ReservationNotifications
import com.nowstation.app.schedule.data.PrefsReservationStore
import com.nowstation.app.schedule.domain.ReservationAlarms
import com.nowstation.app.schedule.domain.ReservationLiveDisplay
import com.nowstation.app.schedule.domain.ReservationStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleModule {
    @Binds
    @Singleton
    abstract fun store(impl: PrefsReservationStore): ReservationStore

    @Binds
    @Singleton
    abstract fun alarms(impl: AndroidReservationAlarms): ReservationAlarms

    @Binds
    abstract fun display(impl: ReservationNotifications): ReservationLiveDisplay
}
