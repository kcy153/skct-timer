package com.example.skcttimer.domain

/**
 * §8-2: 타이머의 시간 산술만 담당하는 순수 상태. 기준 시계는 항상 elapsedRealtime 계열
 * (벽시계 변경/절전에 영향받지 않는 단조 증가 시계)이라고 가정하고 ms 단위 Long으로 받는다.
 *
 * 활성 경과 = now - 시작시각 - 누적 일시정지 시간(일시정지 중이면 정지 시점까지만 반영).
 */
data class ExamClockState(
    val startElapsedMs: Long,
    val pausedAccumMs: Long = 0L,
    val pauseStartElapsedMs: Long? = null,
) {
    val isPaused: Boolean get() = pauseStartElapsedMs != null

    fun activeElapsedMs(nowMs: Long): Long {
        val pausedSoFar = pausedAccumMs + (pauseStartElapsedMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)
        return (nowMs - startElapsedMs - pausedSoFar).coerceAtLeast(0L)
    }

    /** §8-3: 일시정지 — 정지 시점을 기록한다. 이미 일시정지 중이면 그대로 둔다. */
    fun pause(nowMs: Long): ExamClockState {
        if (isPaused) return this
        return copy(pauseStartElapsedMs = nowMs)
    }

    /** §8-3: 재개 — 정지돼 있던 구간을 누적 일시정지 시간에 더한다. */
    fun resume(nowMs: Long): ExamClockState {
        val pauseStart = pauseStartElapsedMs ?: return this
        val elapsedWhilePaused = (nowMs - pauseStart).coerceAtLeast(0L)
        return copy(pausedAccumMs = pausedAccumMs + elapsedWhilePaused, pauseStartElapsedMs = null)
    }

    /**
     * 활성 경과가 activeSec(초)에 도달하는 실제 시각까지 남은 ms.
     * 일시정지 중에는 시간이 흐르지 않으므로 null(예약 불가 — 재개 시 다시 계산해야 함).
     */
    fun delayUntilActiveSecMs(nowMs: Long, activeSec: Int): Long? {
        if (isPaused) return null
        val targetActiveMs = activeSec * 1000L
        val currentActiveMs = activeElapsedMs(nowMs)
        return (targetActiveMs - currentActiveMs).coerceAtLeast(0L)
    }
}
