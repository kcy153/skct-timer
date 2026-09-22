package com.example.skcttimer.domain

enum class AlertKind { PRE, BOUNDARY, MID }

/**
 * @param leadSec PRE에서 경계까지 남은 초(3/2/1). BOUNDARY/MID는 0.
 */
data class Alert(
    val timeSec: Int,
    val kind: AlertKind,
    val leadSec: Int = 0,
)

private val PRE_LEAD_SECONDS = listOf(3, 2, 1)

/**
 * §8-1/§4-1: 타임라인의 모든 "경계"(시험 시작, 영역 종료=휴식 시작, 휴식 종료=다음 영역 시작,
 * 시험 종료)에 대해 B-3/B-2/B-1(PRE)과 B(BOUNDARY)를 만들고, 각 영역 종료 180초 전에
 * MID를 하나씩 추가한다. breakSec=0이면 Break 세그먼트가 없어 "영역 종료"와 "다음 영역 시작"이
 * 같은 시각이 되므로, Set으로 경계 시각을 모아 자연히 중복 제거된다.
 */
fun buildAlerts(timeline: List<Segment>, config: ExamConfig): List<Alert> {
    val boundaries = sortedSetOf<Int>()

    val countdown = timeline.firstOrNull { it.type == SegmentType.COUNTDOWN }
    val sections = timeline.filter { it.type == SegmentType.SECTION }.sortedBy { it.index }
    val breaks = timeline.filter { it.type == SegmentType.BREAK }

    // 1. 시험 시작
    boundaries += countdown?.endSec ?: (sections.firstOrNull()?.startSec ?: 0)

    // 2, 4. 영역 종료(마지막 영역 종료 = 시험 종료도 이 안에 포함됨)
    sections.forEach { boundaries += it.endSec }

    // 3. 휴식 종료 = 다음 영역 시작 (breakSec=0이면 breaks가 비어있어 자동으로 생략됨)
    breaks.forEach { boundaries += it.endSec }

    val alerts = mutableListOf<Alert>()
    for (boundary in boundaries) {
        for (lead in PRE_LEAD_SECONDS) {
            val t = boundary - lead
            if (t >= 0) {
                alerts += Alert(t, AlertKind.PRE, lead)
            }
        }
        alerts += Alert(boundary, AlertKind.BOUNDARY)
    }

    sections.forEach { section ->
        val midTime = section.endSec - config.midAlertLeadSec
        if (midTime > section.startSec) {
            alerts += Alert(midTime, AlertKind.MID)
        }
    }

    return alerts.sortedWith(compareBy({ it.timeSec }, { it.kind.ordinal }))
}
