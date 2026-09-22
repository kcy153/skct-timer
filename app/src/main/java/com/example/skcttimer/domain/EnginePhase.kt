package com.example.skcttimer.domain

/** 타임라인 위의 "지금 어디에 있는가"만 나타낸다. Idle(시작 전)/Paused는 엔진 쪽 상태이고
 * 여기서는 다루지 않는다 — Paused는 계획서 §5대로 실행 상태 위에 얹히는 오버레이라 이 값과
 * 별도 플래그로 관리한다. */
sealed interface EnginePhase {
    data class Active(val segment: Segment) : EnginePhase
    data object Finished : EnginePhase
}

/**
 * activeElapsedSec 시점에 타임라인의 어느 세그먼트에 있는지 계산한다.
 * 세그먼트는 [startSec, endSec) 반열림 구간이라, 경계 정각(B)엔 항상 "다음" 세그먼트로 넘어간다.
 */
fun computePhase(timeline: List<Segment>, activeElapsedSec: Int): EnginePhase {
    val totalSec = timeline.maxOf { it.endSec }
    if (activeElapsedSec >= totalSec) return EnginePhase.Finished
    val segment = timeline.first { activeElapsedSec >= it.startSec && activeElapsedSec < it.endSec }
    return EnginePhase.Active(segment)
}
