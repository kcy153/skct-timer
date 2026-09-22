package com.example.skcttimer.audio

import com.example.skcttimer.data.SettingsRepository
import com.example.skcttimer.domain.Alert

/**
 * §8-5: 소리/진동 스위치가 꺼져 있으면 해당 출력만 건너뛴다 — 타이머 진행(TimerEngine)에는
 * 영향이 없어야 하므로, 이 클래스는 TimerEngine.Listener.onAlert에서 "결과를 통보"만 받고
 * 엔진의 스케줄링 자체에는 관여하지 않는다.
 */
class AlertDispatcher(
    private val toneGenerator: ToneGenerator,
    private val haptics: Haptics,
    private val settings: SettingsRepository,
) {
    fun dispatch(alert: Alert) {
        if (settings.soundEnabled) toneGenerator.play(alert.kind)
        if (settings.vibrationEnabled) haptics.vibrate(alert.kind)
    }
}
