package com.nowstation.app.ride.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.nowstation.app.ride.motion.AccelSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** 가속도 센서(중력 포함) 값을 20Hz로. 열차 가속은 몇 초짜리라 이 정도면 충분하고 배터리를 덜 쓴다. */
class Accelerometer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    fun samples(): Flow<AccelSample> = callbackFlow {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // 센서 시각은 부팅 후 경과 시간이라 벽시계로 바꾼다(묶음으로 늦게 와도 실제 잰 시각을 쓰게)
                val ageNanos = SystemClock.elapsedRealtimeNanos() - event.timestamp
                val at = Instant.now(clock).minusNanos(ageNanos)
                trySend(AccelSample(at, event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble()))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SAMPLING_US, MAX_LATENCY_US)
        awaitClose { manager.unregisterListener(listener) }
    }.buffer(SAMPLES_BUFFER)

    private companion object {
        const val SAMPLING_US = 50_000
        /** 1초씩 묶어서 받아 CPU를 덜 깨운다. 판단 단위(몇 초)보다 짧다. */
        const val MAX_LATENCY_US = 1_000_000
        const val SAMPLES_BUFFER = 256
    }
}
