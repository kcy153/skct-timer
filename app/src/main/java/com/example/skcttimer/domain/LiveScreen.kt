package com.example.skcttimer.domain

/** §5: 지금 어느 화면을 보여줘야 하는지 — 엔진 상태(phase/paused/활성경과)로부터 매 틱 다시 계산한다. */
sealed interface LiveScreen {
    data object Start : LiveScreen

    data class Exam(
        val segment: Segment,
        val remainingSec: Int,
        val paused: Boolean,
        /** §5: 영역 종료 3분 전부터 남은 시간을 앰버로 표시 */
        val isWarning: Boolean,
    ) : LiveScreen

    data class BreakTime(
        val segment: Segment,
        val remainingSec: Int,
        val nextSectionName: String,
        val paused: Boolean,
    ) : LiveScreen

    data object Finished : LiveScreen
}

/**
 * §5 화면 동작 규칙: 남은 시간은 올림(ceil)으로 표시 — 영역 시작 직후 15:00, 마지막 1초는 0:01,
 * 경계 정각에 0:00. 카운트다운도 같은 Exam 화면으로 그린다(별도 화면 없음, §4-1 "3,2,1"은 이
 * 남은시간 표시가 0:03→0:02→0:01로 바뀌는 것뿐).
 */
fun computeLiveScreen(
    phase: EnginePhase,
    activeElapsedMs: Long,
    isPaused: Boolean,
    config: ExamConfig,
): LiveScreen = when (phase) {
    EnginePhase.Finished -> LiveScreen.Finished
    is EnginePhase.Active -> {
        val segment = phase.segment
        val remainingSec = ceilRemainingSec(segment, activeElapsedMs)
        if (segment.type == SegmentType.BREAK) {
            LiveScreen.BreakTime(
                segment = segment,
                remainingSec = remainingSec,
                nextSectionName = nextSectionNameAfterBreak(config, segment),
                paused = isPaused,
            )
        } else {
            LiveScreen.Exam(
                segment = segment,
                remainingSec = remainingSec,
                paused = isPaused,
                isWarning = segment.type == SegmentType.SECTION && remainingSec <= config.midAlertLeadSec,
            )
        }
    }
}

private fun ceilRemainingSec(segment: Segment, activeElapsedMs: Long): Int {
    val remainingMs = (segment.endSec * 1000L - activeElapsedMs).coerceAtLeast(0L)
    return ((remainingMs + 999) / 1000).toInt()
}
