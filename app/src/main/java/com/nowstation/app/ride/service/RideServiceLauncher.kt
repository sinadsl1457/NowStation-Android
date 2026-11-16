package com.nowstation.app.ride.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** ViewModel이 Context를 모르게 서비스 시작을 감싼다(테스트에서는 가짜로). */
interface RideServiceLauncher {
    fun start()
}

@Singleton
class AndroidRideServiceLauncher @Inject constructor(@ApplicationContext private val context: Context) : RideServiceLauncher {
    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, RideService::class.java))
    }
}
