package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugTimeScaleTest {

    @Test
    fun `default config scaled by 60 shrinks 15 minute sections to 15 seconds`() {
        val scaled = ExamConfig().debugScaled(60)

        assertEquals(15, scaled.sectionSec)
        assertEquals(1, scaled.breakSec) // 60/60=1
        assertEquals(1, scaled.countdownSec) // 3/60 -> 0 -> 최소 1로 보정
        assertEquals(3, scaled.midAlertLeadSec) // 180/60=3
    }

    @Test
    fun `breakSec of zero stays zero after scaling`() {
        val scaled = ExamConfig(breakSec = 0).debugScaled(60)

        assertEquals(0, scaled.breakSec)
    }

    @Test
    fun `scaled config still builds a valid non-empty timeline`() {
        val scaled = ExamConfig().debugScaled(60)
        val timeline = buildTimeline(scaled)

        assertEquals(5, timeline.count { it.type == SegmentType.SECTION })
        assertEquals(4, timeline.count { it.type == SegmentType.BREAK })
    }
}
