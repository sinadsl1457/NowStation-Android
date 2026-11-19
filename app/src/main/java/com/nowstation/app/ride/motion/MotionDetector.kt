package com.nowstation.app.ride.motion

import com.nowstation.app.ride.domain.Motion
import com.nowstation.app.ride.domain.MotionKind
import java.time.Instant
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.sqrt

/** 가속도 센서 값 하나(중력 포함, m/s²). [rotation]: 자이로가 잰 회전 빠르기(rad/s), 사람이 폰을 만지는지 가리는 데만 쓴다. */
data class AccelSample(val at: Instant, val x: Double, val y: Double, val z: Double, val rotation: Double = 0.0)

/**
 * 가속도 센서 값에서 열차의 "출발"과 "도착"만 사건으로 뽑는다(순수 Kotlin, 기기 없이 테스트).
 *
 * 가속도 센서는 속도가 바뀌는 순간만 느껴서, 일정한 속도로 달리는 것과 멈춰 있는 것을 구분하지 못한다.
 * 그래서 상태를 재지 않고 사건만 잡는다:
 * - 멈춰 있다가 몇 초 이어지는 꾸준한 밀림 → 출발(MOVING, 밀림이 시작된 시각)
 * - 달리다가 출발과 반대 방향의 꾸준한 밀림(감속)이 끝남 → 도착(STOPPED, 감속이 끝난 시각)
 * 걷기·폰 만지기처럼 크게 흔들리는 동안과, 폰을 돌려 중력 방향이 바뀐 직후는 판단하지 않는다.
 */
class MotionDetector(private val rules: MotionRules = MotionRules()) {

    private var gravity: Vec? = null
    private var push = Vec.ZERO
    private var shake = 0.0
    private var last: Instant? = null
    private var judgeFrom: Instant? = null

    private var moving = false
    private var departureDirection: Vec? = null

    /** 폰을 들거나 내려놓는 중이었다. 끝나면 그때 자세를 새 기준으로 잡는다. */
    private var handling = false

    /** 진행 중인 꾸준한 밀림. */
    private var burstStart: Instant? = null
    /** 마지막으로 기준 이상 밀린 시각. 평활 꼬리(약 1초) 없이 감속이 끝난 시각으로 쓴다. */
    private var burstLastStrong: Instant? = null
    private var burstSum = Vec.ZERO
    private var burstConfirmed = false

    /** 값 하나를 넣고, 사건이 생기면 돌려준다. */
    fun accept(sample: AccelSample): Motion? {
        val raw = Vec(sample.x, sample.y, sample.z)
        val g = gravity
        val prev = last
        last = sample.at
        if (g == null || prev == null) {
            reset(raw, sample.at)
            return null
        }
        // 폰을 돌렸다: 중력을 다시 잡고 잠시 판단하지 않는다
        if (raw.angleDegrees(g) > rules.reorientDegrees) {
            reset(raw, sample.at)
            return null
        }
        val dt = (sample.at.toEpochMilli() - prev.toEpochMilli()) / 1000.0
        if (dt <= 0) return null

        val linear = raw - g
        push = blend(push, linear, dt, rules.pushSmoothingSeconds)
        shake = blend(shake, (linear - push).length, dt, rules.pushSmoothingSeconds)
        // 꾸준한 밀림이 없을 때만 중력을 따라간다(열차 가속이 중력에 섞이지 않게)
        if (push.length < rules.pushEndThreshold) gravity = blend(g, raw, dt, rules.gravitySeconds)

        if (sample.rotation > rules.handlingRotation || shake > rules.maxShakeForJudging) {
            handling = true
            endBurst()
            return null
        }
        if (handling) {
            handling = false
            reset(raw, sample.at)
            return null
        }
        val start = burstStart
        if (start != null && seconds(start, sample.at) > rules.maxPushSeconds) {
            // 가속이 아니라 기울어진 채 그대로였다. 이 밀림으로 낸 "출발"도 틀렸으니 멈춘 것으로 되돌린다
            if (burstConfirmed) moving = false
            reset(raw, sample.at)
            return null
        }
        if (sample.at < judgeFrom) {
            endBurst()
            return null
        }
        return track(sample.at, dt)
    }

    private fun track(at: Instant, dt: Double): Motion? {
        val strength = push.length
        val start = burstStart
        if (start == null) {
            if (strength >= rules.pushThreshold) {
                burstStart = at
                burstLastStrong = at
                burstSum = push * dt
                burstConfirmed = false
            }
            return null
        }
        if (strength < rules.pushThreshold) {
            // 밀림이 끝났다: 확정된 감속이었다면 마지막으로 세게 밀린 순간이 도착
            val ended = burstConfirmed && moving && isOpposite(burstSum)
            val endedAt = burstLastStrong ?: at
            endBurst()
            if (ended) {
                moving = false
                return Motion(MotionKind.STOPPED, endedAt)
            }
            return null
        }
        burstLastStrong = at
        burstSum += push * dt
        if (!burstConfirmed && seconds(start, at) >= rules.minPushSeconds) {
            burstConfirmed = true
            if (!moving) {
                moving = true
                departureDirection = burstSum
                return Motion(MotionKind.MOVING, start)
            }
        }
        return null
    }

    private fun isOpposite(direction: Vec): Boolean {
        val d = departureDirection ?: return false
        return direction.cosine(d) <= rules.oppositeCosine
    }

    private fun reset(raw: Vec, at: Instant) {
        gravity = raw
        push = Vec.ZERO
        shake = 0.0
        judgeFrom = at.plusMillis((rules.warmupSeconds * 1000).toLong())
        endBurst()
    }

    private fun endBurst() {
        burstStart = null
        burstLastStrong = null
        burstSum = Vec.ZERO
        burstConfirmed = false
    }

    private fun seconds(from: Instant, to: Instant) = (to.toEpochMilli() - from.toEpochMilli()) / 1000.0

    /** 지수 평활: 시간 상수 [tau]초. 샘플 간격이 들쭉날쭉해도 같은 속도로 따라간다. */
    private fun blend(old: Vec, new: Vec, dt: Double, tau: Double): Vec {
        val k = 1 - exp(-dt / tau)
        return old + (new - old) * k
    }

    private fun blend(old: Double, new: Double, dt: Double, tau: Double): Double {
        val k = 1 - exp(-dt / tau)
        return old + (new - old) * k
    }
}

internal data class Vec(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Double) = Vec(x * k, y * k, z * k)
    val length: Double get() = sqrt(x * x + y * y + z * z)
    fun dot(o: Vec) = x * o.x + y * o.y + z * o.z
    fun cosine(o: Vec): Double {
        val l = length * o.length
        return if (l == 0.0) 0.0 else dot(o) / l
    }
    fun angleDegrees(o: Vec): Double = Math.toDegrees(acos(cosine(o).coerceIn(-1.0, 1.0)))

    companion object {
        val ZERO = Vec(0.0, 0.0, 0.0)
    }
}
