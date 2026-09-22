package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsTest {

    private fun boundaryTimes(alerts: List<Alert>): List<Int> =
        alerts.filter { it.kind == AlertKind.BOUNDARY }.map { it.timeSec }

    @Test
    fun `default config has exactly 10 boundaries (start 1 + section-end or break-end 8 + exam-end 1)`() {
        val config = ExamConfig()
        val alerts = buildAlerts(buildTimeline(config), config)

        assertEquals(10, boundaryTimes(alerts).size)
    }

    @Test
    fun `every boundary has PRE at B-3,B-2,B-1 and BOUNDARY at B`() {
        val config = ExamConfig()
        val alerts = buildAlerts(buildTimeline(config), config)

        for (boundary in boundaryTimes(alerts)) {
            val preLeads = alerts
                .filter { it.kind == AlertKind.PRE && it.timeSec in (boundary - 3)..(boundary - 1) }
                .map { it.leadSec }
                .sorted()
            assertEquals(
                "boundary $boundary must have PRE at B-3,B-2,B-1",
                listOf(1, 2, 3),
                preLeads,
            )
            assertEquals(
                "boundary $boundary must have exactly one BOUNDARY alert",
                1,
                alerts.count { it.kind == AlertKind.BOUNDARY && it.timeSec == boundary },
            )
        }
    }

    @Test
    fun `MID fires once per section 180 seconds before its end, never inside a break`() {
        val config = ExamConfig()
        val timeline = buildTimeline(config)
        val alerts = buildAlerts(timeline, config)

        val midAlerts = alerts.filter { it.kind == AlertKind.MID }
        assertEquals(5, midAlerts.size)

        val sections = timeline.filter { it.type == SegmentType.SECTION }.sortedBy { it.index }
        val expectedMidTimes = sections.map { it.endSec - config.midAlertLeadSec }
        assertEquals(expectedMidTimes.sorted(), midAlerts.map { it.timeSec }.sorted())

        val breaks = timeline.filter { it.type == SegmentType.BREAK }
        for (mid in midAlerts) {
            assertTrue(
                "MID at ${mid.timeSec} must not fall inside a break segment",
                breaks.none { mid.timeSec >= it.startSec && mid.timeSec < it.endSec },
            )
        }
    }

    @Test
    fun `zero break seconds collapses section-end and next section-start into one boundary, no duplicate alerts`() {
        val config = ExamConfig(breakSec = 0)
        val timeline = buildTimeline(config)
        val alerts = buildAlerts(timeline, config)

        assertTrue(timeline.none { it.type == SegmentType.BREAK })

        // 시작(1) + 영역 종료 5개(마지막이 시험 종료) = 6개, 중복 없음
        val boundaries = boundaryTimes(alerts)
        assertEquals(6, boundaries.size)
        assertEquals(boundaries.size, boundaries.toSet().size)

        // BOUNDARY 알림 자체도 시각별로 정확히 1개씩만 있어야 함(중복 발송 금지, §4-1)
        for (b in boundaries) {
            assertEquals(1, alerts.count { it.kind == AlertKind.BOUNDARY && it.timeSec == b })
        }
    }

    @Test
    fun `break seconds of 10 and 300 keep alert times distinct and non-overlapping`() {
        for (breakSec in listOf(10, ExamConfig.MAX_BREAK_SEC)) {
            val config = ExamConfig(breakSec = breakSec)
            val alerts = buildAlerts(buildTimeline(config), config)

            assertEquals("breakSec=$breakSec", 10, boundaryTimes(alerts).size)

            val midCount = alerts.count { it.kind == AlertKind.MID }
            assertEquals("breakSec=$breakSec", 5, midCount)

            // (timeSec, kind) 조합이 전부 고유해야 함 — 겹치거나 서로를 덮어쓰지 않음
            val keys = alerts.map { it.timeSec to it.kind }
            assertEquals("breakSec=$breakSec", keys.size, keys.toSet().size)
        }
    }

    @Test
    fun `alerts are sorted ascending by time`() {
        for (breakSec in listOf(0, 10, ExamConfig.DEFAULT_BREAK_SEC, ExamConfig.MAX_BREAK_SEC)) {
            val config = ExamConfig(breakSec = breakSec)
            val alerts = buildAlerts(buildTimeline(config), config)

            val times = alerts.map { it.timeSec }
            assertEquals("breakSec=$breakSec", times.sorted(), times)
        }
    }
}
