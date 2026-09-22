package com.example.skcttimer.service

import com.example.skcttimer.domain.Alert
import com.example.skcttimer.domain.AlertKind
import com.example.skcttimer.domain.EnginePhase
import com.example.skcttimer.domain.ExamClockState
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.Segment
import com.example.skcttimer.domain.buildAlerts
import com.example.skcttimer.domain.buildTimeline
import com.example.skcttimer.domain.computePhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * §8-2/§8-3: Idle → Countdown → Section ⇄ Break → Finished 흐름을 진행시키는 엔진.
 * 시간 산술은 [ExamClockState](순수, 테스트됨)에 위임하고, 이 클래스는 그 결과를 바탕으로
 * "다음 알림 시각까지 delay 후 발사"하는 실제 코루틴 루프만 맡는다(고정 주기 폴링 아님, §8-2).
 *
 * 소리/진동 재생은 이 엔진의 책임이 아니다(3단계) — [Listener.onAlert]로 Alert만 통지한다.
 */
class TimerEngine(
    config: ExamConfig,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val listener: Listener,
) {
    interface Listener {
        fun onAlert(alert: Alert)
        fun onPhaseChanged(phase: EnginePhase)
        fun onPauseChanged(paused: Boolean)
    }

    val timeline: List<Segment> = buildTimeline(config)
    private val alerts: List<Alert> = buildAlerts(timeline, config)
    val timelineTotalSec: Int = timeline.maxOf { it.endSec }

    private var clockState: ExamClockState? = null
    private var nextAlertIndex = 0
    private var loopJob: Job? = null

    val isRunning: Boolean get() = clockState != null
    val isPaused: Boolean get() = clockState?.isPaused == true

    fun start() {
        if (clockState != null) return
        clockState = ExamClockState(startElapsedMs = clock.elapsedRealtimeMs())
        nextAlertIndex = 0
        listener.onPhaseChanged(computePhase(timeline, 0))
        launchLoop()
    }

    fun pause() {
        val state = clockState ?: return
        if (state.isPaused) return
        clockState = state.pause(clock.elapsedRealtimeMs())
        loopJob?.cancel()
        loopJob = null
        listener.onPauseChanged(true)
    }

    fun resume() {
        val state = clockState ?: return
        if (!state.isPaused) return
        clockState = state.resume(clock.elapsedRealtimeMs())
        listener.onPauseChanged(false)
        launchLoop()
    }

    /** 시험 중단(뒤로가기 확인 등) 또는 정상 종료 후 정리. */
    fun stop() {
        loopJob?.cancel()
        loopJob = null
        clockState = null
        nextAlertIndex = 0
    }

    fun currentActiveElapsedMs(): Long = clockState?.activeElapsedMs(clock.elapsedRealtimeMs()) ?: 0L

    fun currentPhase(): EnginePhase = computePhase(timeline, (currentActiveElapsedMs() / 1000).toInt())

    private fun launchLoop() {
        loopJob = scope.launch {
            while (nextAlertIndex < alerts.size) {
                val state = clockState ?: return@launch
                val alert = alerts[nextAlertIndex]
                val delayMs = state.delayUntilActiveSecMs(clock.elapsedRealtimeMs(), alert.timeSec)
                    ?: return@launch // 일시정지됨 — resume()이 새 루프를 다시 시작한다
                if (delayMs > 0) delay(delayMs)

                // delay 도중 pause()가 호출됐을 수 있으므로 발사 직전에 다시 확인
                val stateAfterDelay = clockState ?: return@launch
                if (stateAfterDelay.isPaused) return@launch

                listener.onAlert(alert)
                if (alert.kind == AlertKind.BOUNDARY) {
                    listener.onPhaseChanged(computePhase(timeline, alert.timeSec))
                }
                nextAlertIndex++
            }
        }
    }
}
