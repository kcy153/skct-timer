package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentLabelsTest {

    private val config = ExamConfig()

    @Test
    fun `countdown label is fixed text`() {
        val segment = Segment(SegmentType.COUNTDOWN, 0, 0, 3)
        assertEquals("카운트다운", segmentLabel(config, segment))
    }

    @Test
    fun `section label comes from config section names`() {
        val segment = Segment(SegmentType.SECTION, 2, 100, 1000)
        assertEquals("자료해석", segmentLabel(config, segment))
    }

    @Test
    fun `break label is fixed text`() {
        val segment = Segment(SegmentType.BREAK, 1, 900, 960)
        assertEquals("휴식", segmentLabel(config, segment))
    }

    @Test
    fun `next section name after a break is the section right after it`() {
        val break1 = Segment(SegmentType.BREAK, 1, 900, 960) // 1번째 영역 다음 휴식
        assertEquals("자료해석", nextSectionNameAfterBreak(config, break1))

        val break4 = Segment(SegmentType.BREAK, 4, 100, 160) // 4번째 영역 다음 휴식
        assertEquals("수열추리", nextSectionNameAfterBreak(config, break4))
    }
}
