package com.example.skcttimer.domain

/**
 * 시험 진행 규칙. 영역 수/영역 시간/영역 이름/영역 순서는 계획서(§2)에 따라 상수로 고정하고
 * 설정 화면에는 노출하지 않는다 — 바꿀 수 있는 값은 breakSec 하나뿐(§3, §5 설정 화면 세부).
 */
data class ExamConfig(
    val sectionCount: Int = 5,
    val sectionSec: Int = 15 * 60,
    val breakSec: Int = DEFAULT_BREAK_SEC,
    val countdownSec: Int = 3,
    val midAlertLeadSec: Int = 3 * 60,
    val sectionNames: List<String> = DEFAULT_SECTION_NAMES,
) {
    init {
        require(sectionCount >= 1) { "sectionCount must be >= 1" }
        require(sectionSec > 0) { "sectionSec must be > 0" }
        require(breakSec >= 0) { "breakSec must be >= 0" }
        require(countdownSec >= 0) { "countdownSec must be >= 0" }
        require(sectionNames.size == sectionCount) {
            "sectionNames.size(${sectionNames.size}) must equal sectionCount($sectionCount)"
        }
    }

    companion object {
        const val DEFAULT_BREAK_SEC = 60
        const val MIN_BREAK_SEC = 0
        const val MAX_BREAK_SEC = 5 * 60
        const val BREAK_STEP_SEC = 10

        // 나무위키 나열 기준의 가정 순서 — 실제 출제 순서는 미확인(계획서 §2, §12-1).
        // 나중에 바뀌면 이 목록 하나만 고치면 된다.
        val DEFAULT_SECTION_NAMES = listOf("언어이해", "자료해석", "창의수리", "언어추리", "수열추리")
    }
}
