package com.example.skcttimer.service

import com.example.skcttimer.domain.Alert
import com.example.skcttimer.domain.AlertKind
import com.example.skcttimer.domain.EnginePhase
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.SegmentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실제 시간을 기다리지 않고 kotlinx-coroutines-test의 가상 시계로 스케줄링/일시정지/재개를
 * 검증한다 — Clock을 testScheduler.currentTime에 연결해 "실시각"을 통째로 제어한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private class RecordingListener : TimerEngine.Listener {
        val alerts = mutableListOf<Alert>()
        val phases = mutableListOf<EnginePhase>()
        val pauseEvents = mutableListOf<Boolean>()

        override fun onAlert(alert: Alert) {
            alerts += alert
        }

        override fun onPhaseChanged(phase: EnginePhase) {
            phases += phase
        }

        override fun onPauseChanged(paused: Boolean) {
            pauseEvents += paused
        }
    }

    // sectionCount=1, sectionSec=10, breakSec=0, countdown=3, midLead=5 짜리 짧은 커스텀 타임라인.
    // 경계: 시험시작(3), 시험종료(13). 알림: PRE 0,1,2 / BOUNDARY 3 / MID 8 / PRE 10,11,12 / BOUNDARY 13 (총 9개)
    private fun testConfig() = ExamConfig(
        sectionCount = 1,
        sectionSec = 10,
        breakSec = 0,
        countdownSec = 3,
        midAlertLeadSec = 5,
        sectionNames = listOf("테스트영역"),
    )

    private fun TestScope.fakeClock() = Clock { testScheduler.currentTime }

    @Test
    fun `start immediately reports countdown phase`() = runTest {
        val listener = RecordingListener()
        val engine = TimerEngine(testConfig(), fakeClock(), this, listener)

        engine.start()

        assertEquals(1, listener.phases.size)
        val phase = listener.phases[0] as EnginePhase.Active
        assertEquals(SegmentType.COUNTDOWN, phase.segment.type)
        assertTrue(engine.isRunning)
        assertFalse(engine.isPaused)
    }

    @Test
    fun `alerts fire in order at the exact scheduled active-time`() = runTest {
        val listener = RecordingListener()
        val engine = TimerEngine(testConfig(), fakeClock(), this, listener)

        engine.start()
        advanceUntilIdle()

        val expectedTimesAndKinds = listOf(
            0 to AlertKind.PRE,
            1 to AlertKind.PRE,
            2 to AlertKind.PRE,
            3 to AlertKind.BOUNDARY,
            8 to AlertKind.MID,
            10 to AlertKind.PRE,
            11 to AlertKind.PRE,
            12 to AlertKind.PRE,
            13 to AlertKind.BOUNDARY,
        )
        assertEquals(expectedTimesAndKinds, listener.alerts.map { it.timeSec to it.kind })

        // BOUNDARY 2개(시험시작, 시험종료)만큼 phase 변화(+시작 시 즉시 1회) = 3번
        assertEquals(3, listener.phases.size)
        assertTrue(listener.phases.last() is EnginePhase.Finished)
    }

    @Test
    fun `pause stops further alerts and resume continues from the frozen point without drift`() = runTest {
        val listener = RecordingListener()
        val engine = TimerEngine(testConfig(), fakeClock(), this, listener)

        engine.start()
        advanceTimeBy(5_000) // 활성 경과 5초 지점까지: PRE(0,1,2)+BOUNDARY(3) 4건 발사됨
        runCurrent()
        assertEquals(4, listener.alerts.size)

        engine.pause()
        assertTrue(engine.isPaused)
        assertEquals(listOf(true), listener.pauseEvents)

        // 일시정지 중 4초가 더 흘러도(실시각 5000->9000) 알림이 발사되면 안 된다(MID는 8초 지점 예정이었음)
        advanceTimeBy(4_000)
        runCurrent()
        assertEquals(4, listener.alerts.size)

        engine.resume()
        assertEquals(listOf(true, false), listener.pauseEvents)

        // 재개 직후엔 여전히 활성경과 5초 그대로 — MID(8초)까지 3초가 더 필요하다
        advanceTimeBy(2_999)
        runCurrent()
        assertEquals(4, listener.alerts.size) // 아직 MID 발사 전

        advanceTimeBy(1)
        runCurrent()
        assertEquals(5, listener.alerts.size)
        assertEquals(AlertKind.MID, listener.alerts.last().kind)
        assertEquals(8, listener.alerts.last().timeSec)

        // 나머지(PRE 10,11,12 + BOUNDARY 13)까지 전부 발사되고 종료
        advanceUntilIdle()
        assertEquals(9, listener.alerts.size)
        assertTrue(listener.phases.last() is EnginePhase.Finished)
        // Finished가 돼도 엔진이 스스로 stop()하지는 않는다 — 정리는 서비스(리스너) 책임
        assertTrue(engine.isRunning)
    }

    @Test
    fun `pausing twice or resuming while not paused is a no-op`() = runTest {
        val listener = RecordingListener()
        val engine = TimerEngine(testConfig(), fakeClock(), this, listener)

        engine.start()
        engine.resume() // 아직 일시정지 아님 -> no-op
        assertTrue(listener.pauseEvents.isEmpty())

        engine.pause()
        engine.pause() // 이미 일시정지 -> no-op
        assertEquals(listOf(true), listener.pauseEvents)
    }

    @Test
    fun `stop cancels scheduling and resets state so start can run again from zero`() = runTest {
        val listener = RecordingListener()
        val engine = TimerEngine(testConfig(), fakeClock(), this, listener)

        engine.start()
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(4, listener.alerts.size)

        engine.stop()
        assertFalse(engine.isRunning)

        advanceUntilIdle() // 취소됐으니 더 이상 알림이 없어야 함
        assertEquals(4, listener.alerts.size)

        listener.alerts.clear()
        listener.phases.clear()
        engine.start()
        advanceUntilIdle()
        assertEquals(9, listener.alerts.size)
    }
}
