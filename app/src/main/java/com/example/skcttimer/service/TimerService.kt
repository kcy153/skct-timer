package com.example.skcttimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.skcttimer.BuildConfig
import com.example.skcttimer.MainActivity
import com.example.skcttimer.R
import com.example.skcttimer.audio.AlertDispatcher
import com.example.skcttimer.audio.Haptics
import com.example.skcttimer.audio.ToneGenerator
import com.example.skcttimer.data.SettingsRepository
import com.example.skcttimer.domain.Alert
import com.example.skcttimer.domain.EnginePhase
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.Segment
import com.example.skcttimer.domain.debugScaled
import com.example.skcttimer.domain.formatMmSs
import com.example.skcttimer.domain.segmentLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * §8-4: 시험 진행 중 화면이 꺼지거나 앱이 백그라운드로 가도 정확한 시각에 [TimerEngine]이
 * 계속 동작하도록 하는 포그라운드 서비스. [TimerEngine.Listener.onAlert]가 [AlertDispatcher]로
 * 이어져 실제 소리/진동을 낸다(§8-5, 설정의 소리/진동 스위치를 그때그때 확인).
 */
class TimerService : Service() {

    companion object {
        private const val TAG = "TimerService"

        const val ACTION_START = "com.example.skcttimer.action.START"
        const val ACTION_PAUSE = "com.example.skcttimer.action.PAUSE"
        const val ACTION_RESUME = "com.example.skcttimer.action.RESUME"
        const val ACTION_STOP = "com.example.skcttimer.action.STOP"

        const val EXTRA_BREAK_SEC = "breakSec"
        /** 1이면 배율 없음. BuildConfig.DEBUG가 아니면 무시된다(§10-2, 릴리스에는 절대 노출 금지). */
        const val EXTRA_DEBUG_SCALE_DIVISOR = "debugScaleDivisor"

        private const val NOTIFICATION_CHANNEL_ID = "exam_progress"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG = "SkctTimer:ExamWakeLock"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: TimerEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var config: ExamConfig = ExamConfig()

    private lateinit var toneGenerator: ToneGenerator
    private lateinit var haptics: Haptics
    private lateinit var settings: SettingsRepository
    private lateinit var alertDispatcher: AlertDispatcher

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        settings = SettingsRepository(this)
        haptics = Haptics(this)
        toneGenerator = ToneGenerator().also { it.prepare() } // §8-5: 알림 직전이 아니라 미리 생성
        alertDispatcher = AlertDispatcher(toneGenerator, haptics, settings)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> engine?.pause()
            ACTION_RESUME -> engine?.resume()
            ACTION_STOP -> handleStop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        toneGenerator.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun currentEngine(): TimerEngine? = engine
    fun currentConfig(): ExamConfig = config

    private fun handleStart(intent: Intent) {
        if (engine?.isRunning == true) return // 이미 진행 중이면 새로 시작하지 않음

        val breakSec = intent.getIntExtra(EXTRA_BREAK_SEC, settings.breakSec)
        val debugDivisor = intent.getIntExtra(EXTRA_DEBUG_SCALE_DIVISOR, 1)

        var newConfig = ExamConfig(breakSec = breakSec)
        if (BuildConfig.DEBUG && debugDivisor > 1) {
            newConfig = newConfig.debugScaled(debugDivisor)
            Log.d(TAG, "debug time scale x1/$debugDivisor applied (release build never reaches this line)")
        }
        config = newConfig

        val newEngine = TimerEngine(config, SystemElapsedClock, serviceScope, engineListener)
        engine = newEngine

        acquireWakeLock()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(text = "시작하는 중...", useChronometer = false, whenMillis = 0L),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        newEngine.start()
    }

    private fun handleStop() {
        engine?.stop()
        engine = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val engineListener = object : TimerEngine.Listener {
        override fun onAlert(alert: Alert) {
            Log.d(TAG, "alert fired: kind=${alert.kind} timeSec=${alert.timeSec} leadSec=${alert.leadSec}")
            alertDispatcher.dispatch(alert)
        }

        override fun onPhaseChanged(phase: EnginePhase) {
            Log.d(TAG, "phase changed: $phase")
            when (phase) {
                is EnginePhase.Active -> updateNotification(phase.segment)
                EnginePhase.Finished -> handleFinished()
            }
        }

        override fun onPauseChanged(paused: Boolean) {
            Log.d(TAG, "pause changed: $paused")
            val phase = engine?.currentPhase()
            if (phase is EnginePhase.Active) updateNotification(phase.segment)
        }
    }

    private fun handleFinished() {
        // §8-3/§8-4: 시험이 끝나면 웨이크락은 즉시 해제. 포그라운드 알림도 내린다 —
        // 종료를 알리는 건 마지막 BOUNDARY의 경계음/진동(3단계) 몫이다.
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateNotification(segment: Segment) {
        val isPaused = engine?.isPaused == true
        val activeMs = engine?.currentActiveElapsedMs() ?: 0L
        val remainingMs = (segment.endSec * 1000L - activeMs).coerceAtLeast(0L)

        val notification = if (isPaused) {
            val remainingSec = (remainingMs / 1000).toInt()
            buildNotification(
                text = "일시정지 · ${segmentLabel(config, segment)} · 남음 ${formatMmSs(remainingSec)}",
                useChronometer = false,
                whenMillis = 0L,
            )
        } else {
            buildNotification(
                text = segmentLabel(config, segment),
                useChronometer = true,
                whenMillis = System.currentTimeMillis() + remainingMs,
            )
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        text: String,
        useChronometer: Boolean,
        whenMillis: Long,
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("쓱시티 타이머")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setUsesChronometer(useChronometer)
            .apply {
                if (useChronometer) {
                    setWhen(whenMillis)
                    setChronometerCountDown(true)
                }
            }
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "시험 진행 상태",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "현재 영역과 남은 시간을 보여주는 상시 알림입니다. 소리/진동은 여기서 안 냅니다."
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(PLAN_MAX_EXAM_DURATION_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}

/** 79분(계획서 기본 총 시간) + 여유 — 앱이 비정상 종료돼도 웨이크락이 무한정 안 남게 하는 안전장치. */
private const val PLAN_MAX_EXAM_DURATION_MS = 3 * 60 * 60 * 1000L // 3시간
