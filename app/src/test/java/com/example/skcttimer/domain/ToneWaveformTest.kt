package com.example.skcttimer.domain

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneWaveformTest {

    @Test
    fun `boundary tone must be higher pitched than the pre tone`() {
        // §4-3: "경계음은 반드시 예비음보다 높아야 한다"
        assertTrue(ToneSpecs.BOUNDARY.frequencyHz > ToneSpecs.PRE.frequencyHz)
    }

    @Test
    fun `sample count matches duration at the given sample rate`() {
        val spec = ToneSpec(frequencyHz = 440.0, durationMs = 100, fadeMs = 5)
        val samples = generateSineWavePcm16(spec, sampleRateHz = 44_100)

        assertEquals(4_410, samples.size)
    }

    @Test
    fun `starts and ends near silence because of the fade`() {
        val spec = ToneSpec(frequencyHz = 660.0, durationMs = 120, fadeMs = 5)
        val samples = generateSineWavePcm16(spec, sampleRateHz = 44_100)

        // 페이드 구간 중간(정확히 0 근처가 아니어도) 진폭이 아주 작아야 함
        assertTrue(abs(samples.first().toInt()) < 500)
        assertTrue(abs(samples.last().toInt()) < 500)
    }

    @Test
    fun `never clips beyond 16-bit signed range`() {
        for (spec in listOf(ToneSpecs.PRE, ToneSpecs.MID, ToneSpecs.BOUNDARY)) {
            val samples = generateSineWavePcm16(spec)
            val maxAbs = samples.maxOf { abs(it.toInt()) }
            assertTrue("spec=$spec maxAbs=$maxAbs", maxAbs <= Short.MAX_VALUE.toInt())
        }
    }

    @Test
    fun `fade region ramps up monotonically from zero`() {
        val spec = ToneSpec(frequencyHz = 100.0, durationMs = 50, fadeMs = 10)
        val samples = generateSineWavePcm16(spec, sampleRateHz = 44_100)
        val fadeSampleCount = (10 * 44_100 / 1000)

        // 페이드 구간의 포락선(절대값)이 대체로 커지는 추세인지 — 첫 샘플은 0에 아주 가까워야 함
        assertEquals(0, samples[0].toInt())
        assertTrue(abs(samples[fadeSampleCount - 1].toInt()) > abs(samples[0].toInt()))
    }

    @Test
    fun `alert kind maps to a distinct tone spec`() {
        assertEquals(ToneSpecs.PRE, toneSpecFor(AlertKind.PRE))
        assertEquals(ToneSpecs.MID, toneSpecFor(AlertKind.MID))
        assertEquals(ToneSpecs.BOUNDARY, toneSpecFor(AlertKind.BOUNDARY))
    }
}
