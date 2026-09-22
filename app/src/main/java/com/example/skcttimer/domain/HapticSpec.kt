package com.example.skcttimer.domain

/**
 * §4-3: 진동 패턴. ms 단위 on/off 교대 목록으로, 첫 값부터 켜짐(off 선행 없음) —
 * android.os.VibrationEffect.createWaveform(patternMs.toLongArray(), -1)에 그대로 쓸 수 있는 모양.
 */
data class HapticSpec(val patternMs: List<Long>)

object HapticSpecs {
    val PRE = HapticSpec(listOf(0L, 80L))       // 짧게
    val MID = HapticSpec(listOf(0L, 200L))      // 중간
    val BOUNDARY = HapticSpec(listOf(0L, 500L)) // 길게
}

fun hapticSpecFor(kind: AlertKind): HapticSpec = when (kind) {
    AlertKind.PRE -> HapticSpecs.PRE
    AlertKind.MID -> HapticSpecs.MID
    AlertKind.BOUNDARY -> HapticSpecs.BOUNDARY
}
