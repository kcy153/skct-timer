package com.example.skcttimer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `formats minutes and seconds with zero-padded seconds`() {
        assertEquals("15:00", formatMmSs(900))
        assertEquals("0:03", formatMmSs(3))
        assertEquals("0:00", formatMmSs(0))
        assertEquals("1:05", formatMmSs(65))
    }

    @Test
    fun `negative input clamps to zero instead of showing a negative time`() {
        assertEquals("0:00", formatMmSs(-5))
    }
}
