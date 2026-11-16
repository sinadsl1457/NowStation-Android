package com.nowstation.app.ride.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RideServiceModule {
    @Binds
    abstract fun launcher(impl: AndroidRideServiceLauncher): RideServiceLauncher
}
