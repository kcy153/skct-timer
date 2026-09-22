package com.example.skcttimer.domain

/**
 * §10-2: 디버그 빌드 전용 "빠른 테스트" 배율 — 모든 시간을 1/divisor로 줄여서
 * Idle → 종료까지 전체 흐름을 몇 초~몇십 초 안에 검증할 수 있게 한다.
 * 릴리스 빌드/설정 화면에는 절대 노출하지 않는다(계획서 §10-2, 호출부에서 BuildConfig.DEBUG로 가드).
 */
fun ExamConfig.debugScaled(divisor: Int = 60): ExamConfig {
    require(divisor >= 1) { "divisor must be >= 1" }
    return copy(
        sectionSec = (sectionSec / divisor).coerceAtLeast(1),
        // breakSec=0("휴식 없음")은 배율을 적용해도 0으로 남아야 한다 — 0/60=0을 그대로 두되,
        // 0이 아닌 값은 최소 1초는 보장한다.
        breakSec = if (breakSec == 0) 0 else (breakSec / divisor).coerceAtLeast(1),
        countdownSec = (countdownSec / divisor).coerceAtLeast(1),
        midAlertLeadSec = (midAlertLeadSec / divisor).coerceAtLeast(1),
    )
}
