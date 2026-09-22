package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticSpecTest {

    @Test
    fun `all patterns start on immediately (no leading delay)`() {
        for (spec in listOf(HapticSpecs.PRE, HapticSpecs.MID, HapticSpecs.BOUNDARY)) {
            assertEquals(0L, spec.patternMs.first())
        }
    }

    @Test
    fun `pre is shorter than mid, mid is shorter than boundary`() {
        // §4-3: 예비(짧게) < 3분 전(중간) < 경계(길게)
        val preOnMs = HapticSpecs.PRE.patternMs[1]
        val midOnMs = HapticSpecs.MID.patternMs[1]
        val boundaryOnMs = HapticSpecs.BOUNDARY.patternMs[1]

        assertTrue(preOnMs < midOnMs)
        assertTrue(midOnMs < boundaryOnMs)
    }

    @Test
    fun `alert kind maps to a distinct haptic spec`() {
        assertEquals(HapticSpecs.PRE, hapticSpecFor(AlertKind.PRE))
        assertEquals(HapticSpecs.MID, hapticSpecFor(AlertKind.MID))
        assertEquals(HapticSpecs.BOUNDARY, hapticSpecFor(AlertKind.BOUNDARY))
    }
}
