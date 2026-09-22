package com.example.skcttimer.data

import android.content.Context
import android.content.SharedPreferences
import com.example.skcttimer.domain.ExamConfig

/**
 * §5 설정 화면 저장소. 지금은 3단계(소리/진동)에 필요한 두 스위치까지만 실제로 쓰이고,
 * breakSec은 4단계(UI, 설정 화면)에서 시작 화면/설정 화면이 붙을 때 실제로 읽고 쓰게 된다.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var breakSec: Int
        get() = prefs.getInt(KEY_BREAK_SEC, ExamConfig.DEFAULT_BREAK_SEC)
        set(value) = prefs.edit().putInt(
            KEY_BREAK_SEC,
            value.coerceIn(ExamConfig.MIN_BREAK_SEC, ExamConfig.MAX_BREAK_SEC),
        ).apply()

    /** §8-4: 알림 권한을 거부했을 때 "안내를 한 번 보여준다" — 세션이 아니라 설치 전체 기준 한 번. */
    var notificationPermissionNoticeShown: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_NOTICE_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_NOTICE_SHOWN, value).apply()

    companion object {
        private const val PREFS_NAME = "skct_timer_settings"
        private const val KEY_SOUND_ENABLED = "soundEnabled"
        private const val KEY_VIBRATION_ENABLED = "vibrationEnabled"
        private const val KEY_BREAK_SEC = "breakSec"
        private const val KEY_NOTIFICATION_NOTICE_SHOWN = "notificationPermissionNoticeShown"
    }
}
