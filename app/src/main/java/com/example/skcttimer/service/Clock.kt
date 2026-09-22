package com.example.skcttimer.service

/** §8-2: 기준 시계 추상화 — 실제로는 SystemClock.elapsedRealtime()(단조 증가, 절전 무관)을 쓰고,
 * 테스트에서는 TestScope의 가상 시계를 주입할 수 있게 인터페이스로 분리한다. */
fun interface Clock {
    fun elapsedRealtimeMs(): Long
}
