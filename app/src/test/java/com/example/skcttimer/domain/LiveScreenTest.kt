package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScreenTest {

    private val config = ExamConfig() // sectionSec=900, midAlertLeadSec=180

    @Test
    fun `finished phase always maps to Finished regardless of other params`() {
        val screen = computeLiveScreen(EnginePhase.Finished, activeElapsedMs = 12345, isPaused = true, config)
        assertEquals(LiveScreen.Finished, screen)
    }

    @Test
    fun `section start shows the full duration, ceil-rounded`() {
        val section = Segment(SegmentType.SECTION, 1, startSec = 3, endSec = 903)
        val screen = computeLiveScreen(
            EnginePhase.Active(section),
            activeElapsedMs = 3_000L, // 딱 시작 시각
            isPaused = false,
            config,
        ) as LiveScreen.Exam

        assertEquals(900, screen.remainingSec) // 15:00
        assertFalse(screen.isWarning)
    }

    @Test
    fun `remaining seconds ceil at exact boundary and one ms before it`() {
        val section = Segment(SegmentType.SECTION, 1, startSec = 0, endSec = 10)

        val atBoundary = computeLiveScreen(EnginePhase.Active(section), 10_000L, false, config) as LiveScreen.Exam
        assertEquals(0, atBoundary.remainingSec)

        val oneMsBefore = computeLiveScreen(EnginePhase.Active(section), 9_999L, false, config) as LiveScreen.Exam
        assertEquals(1, oneMsBefore.remainingSec)

        val exactlyOneSecBefore = computeLiveScreen(EnginePhase.Active(section), 9_000L, false, config) as LiveScreen.Exam
        assertEquals(1, exactlyOneSecBefore.remainingSec)
    }

    @Test
    fun `warning turns on at exactly 180 seconds left and off at 181`() {
        val section = Segment(SegmentType.SECTION, 1, startSec = 0, endSec = 900)

        val at180 = computeLiveScreen(EnginePhase.Active(section), (900 - 180) * 1000L, false, config) as LiveScreen.Exam
        assertTrue(at180.isWarning)

        val at181 = computeLiveScreen(EnginePhase.Active(section), (900 - 181) * 1000L, false, config) as LiveScreen.Exam
        assertFalse(at181.isWarning)
    }

    @Test
    fun `countdown segment renders as Exam too, never a separate screen`() {
        val countdown = Segment(SegmentType.COUNTDOWN, 0, 0, 3)
        val screen = computeLiveScreen(EnginePhase.Active(countdown), 1_000L, false, config)

        assertTrue(screen is LiveScreen.Exam)
        assertEquals(SegmentType.COUNTDOWN, (screen as LiveScreen.Exam).segment.type)
    }

    @Test
    fun `break segment maps to BreakTime with the correct next section name`() {
        val breakSegment = Segment(SegmentType.BREAK, 2, startSec = 100, endSec = 160)
        val screen = computeLiveScreen(EnginePhase.Active(breakSegment), 130_000L, isPaused = true, config)

        assertTrue(screen is LiveScreen.BreakTime)
        val breakScreen = screen as LiveScreen.BreakTime
        assertEquals(30, breakScreen.remainingSec)
        assertEquals("창의수리", breakScreen.nextSectionName) // break.index=2 다음은 3번째 영역
        assertTrue(breakScreen.paused)
    }

    @Test
    fun `paused flag passes through unchanged`() {
        val section = Segment(SegmentType.SECTION, 1, 0, 900)
        val screen = computeLiveScreen(EnginePhase.Active(section), 0L, isPaused = true, config) as LiveScreen.Exam
        assertTrue(screen.paused)
    }
}
