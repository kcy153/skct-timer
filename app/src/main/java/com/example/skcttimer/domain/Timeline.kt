package com.example.skcttimer.domain

enum class SegmentType { COUNTDOWN, SECTION, BREAK }

/**
 * 시험 전체 타임라인의 한 구간. index는 COUNTDOWN이면 0, SECTION/BREAK면 1부터 시작하는
 * 순번(BREAK의 index=i는 "i번째 영역 다음 휴식"을 뜻함, 1..sectionCount-1).
 */
data class Segment(
    val type: SegmentType,
    val index: Int,
    val startSec: Int,
    val endSec: Int,
) {
    val durationSec: Int get() = endSec - startSec
}

/**
 * §8-1: Countdown → (Section → Break?) × sectionCount 순서로 배치한다.
 * breakSec이 0이면 Break 세그먼트를 아예 만들지 않는다(경계 중복 방지, §4-1 마지막 항목).
 */
fun buildTimeline(config: ExamConfig): List<Segment> {
    val segments = mutableListOf<Segment>()
    var cursor = 0

    if (config.countdownSec > 0) {
        segments += Segment(SegmentType.COUNTDOWN, 0, cursor, cursor + config.countdownSec)
        cursor += config.countdownSec
    }

    for (i in 1..config.sectionCount) {
        segments += Segment(SegmentType.SECTION, i, cursor, cursor + config.sectionSec)
        cursor += config.sectionSec

        val isLastSection = i == config.sectionCount
        if (!isLastSection && config.breakSec > 0) {
            segments += Segment(SegmentType.BREAK, i, cursor, cursor + config.breakSec)
            cursor += config.breakSec
        }
    }

    return segments
}
