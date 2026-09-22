package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {

    @Test
    fun `default config total length is 3 plus 4500 plus 240 seconds`() {
        val timeline = buildTimeline(ExamConfig())

        val totalSec = timeline.maxOf { it.endSec }

        assertEquals(3 + 4500 + 240, totalSec)
    }

    @Test
    fun `default config has 5 sections and 4 breaks in order`() {
        val timeline = buildTimeline(ExamConfig())

        assertEquals(SegmentType.COUNTDOWN, timeline[0].type)
        assertEquals(0, timeline[0].startSec)
        assertEquals(3, timeline[0].endSec)

        val sections = timeline.filter { it.type == SegmentType.SECTION }
        val breaks = timeline.filter { it.type == SegmentType.BREAK }
        assertEquals(5, sections.size)
        assertEquals(4, breaks.size)

        // 세그먼트가 끊김/겹침 없이 이어 붙는지(각 세그먼트의 끝 == 다음 세그먼트의 시작)
        for (i in 0 until timeline.size - 1) {
            assertEquals(
                "segment $i end must equal segment ${i + 1} start",
                timeline[i].endSec,
                timeline[i + 1].startSec,
            )
        }
    }

    @Test
    fun `zero break seconds produces no break segments`() {
        val timeline = buildTimeline(ExamConfig(breakSec = 0))

        assertTrue(timeline.none { it.type == SegmentType.BREAK })
        // Countdown(3) + Section(900) * 5, 끊김 없이 이어붙음
        assertEquals(3 + 900 * 5, timeline.maxOf { it.endSec })
    }

    @Test
    fun `custom break seconds changes total length accordingly`() {
        val timeline300 = buildTimeline(ExamConfig(breakSec = 300))
        assertEquals(3 + 4500 + 300 * 4, timeline300.maxOf { it.endSec })

        val timeline10 = buildTimeline(ExamConfig(breakSec = 10))
        assertEquals(3 + 4500 + 10 * 4, timeline10.maxOf { it.endSec })
    }
}
