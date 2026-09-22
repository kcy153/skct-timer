package com.example.skcttimer.domain

/** 화면(§5)과 알림(§8-4)이 공유하는 세그먼트 표시 이름. */
fun segmentLabel(config: ExamConfig, segment: Segment): String = when (segment.type) {
    SegmentType.COUNTDOWN -> "카운트다운"
    SegmentType.SECTION -> config.sectionNames.getOrElse(segment.index - 1) { "영역 ${segment.index}" }
    SegmentType.BREAK -> "휴식"
}

/** 휴식 화면의 "다음: OO" 표시용 — 이 휴식 다음에 시작될 영역 이름. */
fun nextSectionNameAfterBreak(config: ExamConfig, breakSegment: Segment): String {
    require(breakSegment.type == SegmentType.BREAK) { "not a break segment: $breakSegment" }
    return config.sectionNames.getOrElse(breakSegment.index) { "영역 ${breakSegment.index + 1}" }
}
