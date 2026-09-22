package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnginePhaseTest {

    @Test
    fun `phase at time zero is the countdown segment`() {
        val config = ExamConfig()
        val timeline = buildTimeline(config)

        val phase = computePhase(timeline, 0)

        assertTrue(phase is EnginePhase.Active)
        assertEquals(SegmentType.COUNTDOWN, (phase as EnginePhase.Active).segment.type)
    }

    @Test
    fun `phase transitions exactly at each boundary, half-open on the right`() {
        val config = ExamConfig()
        val timeline = buildTimeline(config)
        val countdownEnd = timeline.first { it.type == SegmentType.COUNTDOWN }.endSec

        val justBefore = computePhase(timeline, countdownEnd - 1) as EnginePhase.Active
        val atBoundary = computePhase(timeline, countdownEnd) as EnginePhase.Active

        assertEquals(SegmentType.COUNTDOWN, justBefore.segment.type)
        assertEquals(SegmentType.SECTION, atBoundary.segment.type)
        assertEquals(1, atBoundary.segment.index)
    }

    @Test
    fun `phase is finished once total time is reached`() {
        val config = ExamConfig()
        val timeline = buildTimeline(config)
        val totalSec = timeline.maxOf { it.endSec }

        assertTrue(computePhase(timeline, totalSec) is EnginePhase.Finished)
        assertTrue(computePhase(timeline, totalSec + 100) is EnginePhase.Finished)
        assertTrue(computePhase(timeline, totalSec - 1) is EnginePhase.Active)
    }

    @Test
    fun `phase walks through every section and break in order`() {
        val config = ExamConfig()
        val timeline = buildTimeline(config)

        val observedOrder = timeline.map {
            computePhase(timeline, it.startSec).let { p -> (p as EnginePhase.Active).segment.type to p.segment.index }
        }
        val expectedOrder = timeline.map { it.type to it.index }

        assertEquals(expectedOrder, observedOrder)
    }
}
