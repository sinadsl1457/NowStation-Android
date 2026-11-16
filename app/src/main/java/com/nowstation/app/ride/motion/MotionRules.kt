package com.nowstation.app.ride.motion

/**
 * 움직임 판단 기준값. 실기기 없이(합성 신호·에뮬레이터) 정한 값이라 실제 지하철에서 다시 맞춰야 한다.
 * 근거: 지하철 가속·감속은 보통 0.5~1.3 m/s²로 몇~수십 초 이어진다. 걷기는 1~3 m/s²로 1초에 두 번쯤 흔들린다.
 */
data class MotionRules(
    /** 이 이상 꾸준히 밀리면 가속(감속) 중으로 본다(m/s²). */
    val pushThreshold: Double = 0.25,
    /** 이 아래면 꾸준한 밀림이 없다고 보고 중력을 따라간다(m/s²). */
    val pushEndThreshold: Double = 0.15,
    /** 꾸준한 밀림이 이만큼 이어져야 출발·감속으로 인정한다(초). 짧은 흔들림·기울임을 거른다. */
    val minPushSeconds: Double = 4.0,
    /** 짧은 흔들림이 이보다 크면 걷는 중·폰을 만지는 중이라 판단하지 않는다(m/s²). */
    val maxShakeForJudging: Double = 0.8,
    /** 중력 방향이 이보다 크게 바뀌면 폰을 돌린 것(열차 가속으로는 최대 약 9°). 중력을 다시 잡는다(도). */
    val reorientDegrees: Double = 20.0,
    /** 꾸준한 밀림을 보는 평활 시간(초). 이보다 빠른 흔들림은 걸러진다. */
    val pushSmoothingSeconds: Double = 1.0,
    /** 중력을 따라가는 속도(초). 밀림이 없을 때만 따라간다. */
    val gravitySeconds: Double = 5.0,
    /** 감속을 출발과 반대 방향으로 볼 기준(방향 코사인). */
    val oppositeCosine: Double = -0.3,
    /** 시작 직후 중력이 자리 잡을 때까지 판단하지 않는다(초). */
    val warmupSeconds: Double = 3.0,
)
