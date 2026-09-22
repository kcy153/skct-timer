package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamClockStateTest {

    @Test
    fun `active elapsed grows with wall clock when running`() {
        val state = ExamClockState(startElapsedMs = 1_000L)

        assertEquals(0L, state.activeElapsedMs(1_000L))
        assertEquals(2_500L, state.activeElapsedMs(3_500L))
        assertFalse(state.isPaused)
    }

    @Test
    fun `pause freezes active elapsed at the pause instant`() {
        val running = ExamClockState(startElapsedMs = 0L)
        val paused = running.pause(5_000L)

        assertTrue(paused.isPaused)
        assertEquals(5_000L, paused.activeElapsedMs(5_000L))
        // 일시정지 중엔 시간이 더 흘러도(now가 커져도) 활성 경과는 그대로다
        assertEquals(5_000L, paused.activeElapsedMs(20_000L))
    }

    @Test
    fun `pause is idempotent`() {
        val running = ExamClockState(startElapsedMs = 0L)
        val pausedOnce = running.pause(5_000L)
        val pausedTwice = pausedOnce.pause(9_000L)

        assertEquals(pausedOnce, pausedTwice)
    }

    @Test
    fun `resume accumulates paused duration and lets active elapsed continue from where it stopped`() {
        val state = ExamClockState(startElapsedMs = 0L)
            .pause(5_000L) // 5초 지점에서 정지
        val resumed = state.resume(15_000L) // 10초간 정지해 있다가 재개

        assertFalse(resumed.isPaused)
        // 재개 직후엔 여전히 5초(정지 구간은 활성 경과에서 빠짐)
        assertEquals(5_000L, resumed.activeElapsedMs(15_000L))
        // 재개 후 3초가 더 지나면 8초
        assertEquals(8_000L, resumed.activeElapsedMs(18_000L))
    }

    @Test
    fun `resume without pause is a no-op`() {
        val running = ExamClockState(startElapsedMs = 0L)
        assertEquals(running, running.resume(10_000L))
    }

    @Test
    fun `multiple pause-resume cycles accumulate correctly`() {
        var state = ExamClockState(startElapsedMs = 0L)
        state = state.pause(1_000L).resume(3_000L) // 2초 정지
        state = state.pause(5_000L).resume(9_000L) // 4초 정지 (총 6초 정지)

        // 실제 시각 9_000ms, 시작 0ms, 총 정지 6_000ms => 활성 경과 3_000ms
        assertEquals(3_000L, state.activeElapsedMs(9_000L))
    }

    @Test
    fun `delayUntilActiveSecMs returns remaining ms to reach target while running`() {
        val state = ExamClockState(startElapsedMs = 0L)

        assertEquals(5_000L, state.delayUntilActiveSecMs(nowMs = 0L, activeSec = 5))
        assertEquals(2_000L, state.delayUntilActiveSecMs(nowMs = 3_000L, activeSec = 5))
        // 이미 지난 시각이면 0(즉시 발사)
        assertEquals(0L, state.delayUntilActiveSecMs(nowMs = 9_000L, activeSec = 5))
    }

    @Test
    fun `delayUntilActiveSecMs is null while paused`() {
        val state = ExamClockState(startElapsedMs = 0L).pause(2_000L)

        assertNull(state.delayUntilActiveSecMs(nowMs = 2_000L, activeSec = 5))
    }
}
