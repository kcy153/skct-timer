package com.example.skcttimer.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.skcttimer.domain.AlertKind
import com.example.skcttimer.domain.hapticSpecFor

/** §5 설정 화면: 진동 미지원 기기에서는 [isSupported]로 걸러서 스위치를 비활성화한다(4단계 UI 몫). */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val isSupported: Boolean = vibrator?.hasVibrator() == true

    fun vibrate(kind: AlertKind) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val spec = hapticSpecFor(kind)
        v.vibrate(VibrationEffect.createWaveform(spec.patternMs.toLongArray(), -1))
    }
}
