package com.nowstation.app.core

import com.nowstation.app.ride.data.RideRepositoryImpl
import com.nowstation.app.ride.domain.RideRepository
import com.nowstation.app.station.data.StationRepositoryImpl
import com.nowstation.app.station.domain.StationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun stationRepository(impl: StationRepositoryImpl): StationRepository

    @Binds
    @Singleton
    abstract fun rideRepository(impl: RideRepositoryImpl): RideRepository

    companion object {
        /** 시간은 주입받는다(테스트에서 바꿔 끼움). */
        @Provides
        @Singleton
        fun clock(): Clock = Clock.systemUTC()
    }
}
