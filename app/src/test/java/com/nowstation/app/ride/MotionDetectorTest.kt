package com.nowstation.app.ride

import com.nowstation.app.ride.domain.Motion
import com.nowstation.app.ride.domain.MotionKind
import com.nowstation.app.ride.motion.AccelSample
import com.nowstation.app.ride.motion.MotionDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 합성 신호로 확인한다(20Hz, 폰은 세로로 들고 화면이 앞). 중력 9.8은 y, 열차 진행은 x 방향.
 * 실제 지하철 값이 아니므로 기준값(MotionRules)은 실기기에서 다시 맞춘다.
 */
class MotionDetectorTest {
    private val t0 = Instant.parse("2026-11-17T08:00:00Z")
    private val random = Random(7)

    /** [from]~[to]초 동안 [accel](초 → x 방향 가속)과 작은 덜컹임을 더한 값. */
    private fun signal(from: Double, to: Double, accel: (Double) -> Double = { 0.0 }, extra: (Double) -> Triple<Double, Double, Double> = { Triple(0.0, 0.0, 0.0) }) =
        generateSequence(from) { it + 0.05 }.takeWhile { it < to }.map { s ->
            val (ex, ey, ez) = extra(s)
            AccelSample(t0.plusMillis((s * 1000).toLong()), accel(s) + ex + noise(), 9.8 + ey + noise(), ez + noise())
        }.toList()

    private fun noise() = (random.nextDouble() - 0.5) * 0.1

    private fun run(samples: List<AccelSample>): List<Motion> {
        val d = MotionDetector()
        return samples.mapNotNull(d::accept)
    }

    private fun at(seconds: Double): Instant = t0.plusMillis((seconds * 1000).toLong())

    private fun assertNear(expected: Instant, actual: Instant, toleranceSeconds: Double = 1.0) =
        assertTrue("$actual ≈ $expected", kotlin.math.abs(actual.toEpochMilli() - expected.toEpochMilli()) <= toleranceSeconds * 1000)

    /** 한 정거장: 10~20초 출발 가속, 20~70초 일정 속도, 70~80초 감속, 그 뒤 정차. */
    private fun oneStop(): (Double) -> Double = { s ->
        when {
            s in 10.0..20.0 -> 0.9
            s in 70.0..80.0 -> -0.9
            else -> 0.0
        }
    }

    @Test
    fun `가만히 있으면 아무 일도 없다`() {
        assertTrue(run(signal(0.0, 60.0)).isEmpty())
    }

    @Test
    fun `출발 가속은 시작 시각으로, 반대 방향 감속은 끝난 시각으로 잡는다`() {
        val events = run(signal(0.0, 100.0, oneStop()))

        assertEquals(listOf(MotionKind.MOVING, MotionKind.STOPPED), events.map { it.kind })
        assertNear(at(10.0), events[0].at, 1.5)
        assertNear(at(80.0), events[1].at, 1.5)
    }

    @Test
    fun `두 정거장 연속(출발·도착·출발·도착)`() {
        val twoStops: (Double) -> Double = { s ->
            when {
                s in 10.0..20.0 -> 0.9
                s in 60.0..70.0 -> -0.9
                s in 100.0..110.0 -> 0.9
                s in 150.0..160.0 -> -0.9
                else -> 0.0
            }
        }
        val events = run(signal(0.0, 180.0, twoStops))

        assertEquals(listOf(MotionKind.MOVING, MotionKind.STOPPED, MotionKind.MOVING, MotionKind.STOPPED), events.map { it.kind })
    }

    @Test
    fun `걷는 흔들림(2Hz, 2m per s2)은 출발로 보지 않는다`() {
        val walking = signal(0.0, 60.0, extra = { s -> Triple(0.0, 2.0 * sin(2 * PI * 2 * s), 1.0 * cos(2 * PI * 2 * s)) })

        assertTrue(run(walking).isEmpty())
    }

    @Test
    fun `짧게 덜컹이거나 잠깐 밀리는 건(2초) 출발이 아니다`() {
        assertTrue(run(signal(0.0, 40.0, { s -> if (s in 10.0..12.0) 0.9 else 0.0 })).isEmpty())
    }

    @Test
    fun `폰을 돌려 중력 방향이 바뀌면 출발로 보지 않는다`() {
        // 20초에 폰을 옆으로 눕힘: 중력이 y에서 x로
        val samples = signal(0.0, 60.0).map { s ->
            if (s.at >= at(20.0)) s.copy(x = 9.8 + (s.x), y = s.y - 9.8) else s
        }

        assertTrue(run(samples).isEmpty())
    }

    @Test
    fun `달리는 중 같은 방향으로 한 번 더 가속해도 도착으로 보지 않는다`() {
        val events = run(signal(0.0, 120.0, { s -> if (s in 10.0..20.0 || s in 50.0..58.0) 0.9 else 0.0 }))

        assertEquals(listOf(MotionKind.MOVING), events.map { it.kind })
    }

    // 실기기(iOS)에서 발견: 탑승을 시작하고 폰을 책상에 내려놓았더니 가만히 있는데 "출발"로 판단해 엉뚱한 열차를 따라감
    private fun tilted(seconds: Double, deg: (Double) -> Double, rotation: (Double) -> Double = { 0.0 }, extra: (Double) -> Triple<Double, Double, Double> = { Triple(0.0, 0.0, 0.0) }) =
        generateSequence(0.0) { it + 0.05 }.takeWhile { it < seconds }.map { s ->
            val r = Math.toRadians(deg(s))
            val (ex, ey, ez) = extra(s)
            AccelSample(at(s), 9.8 * sin(r) + ex, 9.8 * cos(r) + ey, ez, rotation = rotation(s))
        }.toList()

    @Test
    fun `손에 들었다가 조금 기울여 책상에 내려놓으면 출발이 아니다`() {
        val samples = tilted(
            120.0,
            deg = { s -> if (s < 10) 0.0 else if (s < 11.5) (s - 10) / 1.5 * 8 else 8.0 },
            extra = { s -> if (s in 10.0..11.5) Triple(1.5 * sin(2 * PI * 6 * s), 0.0, 0.75 * sin(2 * PI * 6 * s)) else Triple(0.0, 0.0, 0.0) },
        )
        assertTrue(run(samples).isEmpty())
    }

    @Test
    fun `흔들림 없이 살살 돌려 내려놓아도 회전으로 알아채 출발이 아니다`() {
        val samples = tilted(
            90.0,
            deg = { s -> if (s < 10) 0.0 else if (s < 11) (s - 10) * 10 else 10.0 },
            rotation = { s -> if (s in 10.0..11.0) 0.6 else 0.0 },
        )
        assertTrue(run(samples).isEmpty())
    }

    @Test
    fun `천천히 기울어 굳어도 40초 뒤에는 풀리고 진짜 출발을 잡는다`() {
        val samples = tilted(
            200.0,
            deg = { s -> if (s < 10) 0.0 else if (s < 20) (s - 10) * 0.6 else 6.0 },
            extra = { s -> Triple(0.0, 0.0, if (s in 150.0..160.0) 0.9 else 0.0) },
        )
        val last = run(samples).last()
        assertEquals(MotionKind.MOVING, last.kind)
        assertNear(at(150.0), last.at, 1.5)
    }
}
