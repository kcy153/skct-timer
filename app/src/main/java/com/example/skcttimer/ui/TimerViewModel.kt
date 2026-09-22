package com.example.skcttimer.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.skcttimer.data.SettingsRepository
import com.example.skcttimer.domain.LiveScreen
import com.example.skcttimer.domain.computeLiveScreen
import com.example.skcttimer.service.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * §8-2: "서비스와 UI가 각자 시간을 세지 않고 같은 시작시각/일시정지 상태를 공유한다" —
 * 이 ViewModel은 자체 시계를 갖지 않고, 약 100ms마다 [TimerService]에 바인딩된 [TimerEngine]을
 * 읽어([LiveScreen]으로 변환) 화면에 뿌리기만 한다.
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    val settings = SettingsRepository(application)

    private val _liveScreen = MutableStateFlow<LiveScreen>(LiveScreen.Start)
    val liveScreen: StateFlow<LiveScreen> = _liveScreen.asStateFlow()

    private var binder: TimerService.LocalBinder? = null
    private var tickJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as? TimerService.LocalBinder
            startTicking()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    init {
        // 앱 프로세스가 재시작됐는데 시험이 이미 백그라운드에서 진행 중이면(§8-4) 새로 시작하지
        // 않고 그 서비스에 그대로 다시 붙는다 — 실행 중이 아니면 flags=0이라 아무 일도 안 생김.
        getApplication<Application>().bindService(serviceIntent(), connection, 0)
    }

    fun startExam(debugScaleDivisor: Int = 1) {
        val app = getApplication<Application>()
        val intent = serviceIntent().apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_BREAK_SEC, settings.breakSec)
            putExtra(TimerService.EXTRA_DEBUG_SCALE_DIVISOR, debugScaleDivisor)
        }
        ContextCompat.startForegroundService(app, intent)
        if (binder == null) {
            app.bindService(serviceIntent(), connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun pause() = sendAction(TimerService.ACTION_PAUSE)
    fun resume() = sendAction(TimerService.ACTION_RESUME)

    /** 시험 중단 확인(뒤로가기) 또는 종료 화면의 "처음으로"에서 호출 — 서비스를 완전히 정리한다. */
    fun stopAndReturnToStart() {
        sendAction(TimerService.ACTION_STOP)
        stopTicking()
        unbindQuietly()
        _liveScreen.value = LiveScreen.Start
    }

    private fun sendAction(action: String) {
        // startForegroundService가 아니라 startService — 이미 실행 중인 서비스에 명령만 전달하는
        // 용도라, Finished 이후(포그라운드 아닌 상태)에 호출돼도 새 foreground 의무가 안 생긴다.
        val app = getApplication<Application>()
        app.startService(serviceIntent().apply { this.action = action })
    }

    private fun startTicking() {
        stopTicking()
        tickJob = viewModelScope.launch {
            while (isActive) {
                updateScreenFromEngine()
                delay(100)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun updateScreenFromEngine() {
        val service = binder?.getService()
        val engine = service?.currentEngine()
        _liveScreen.value = if (service == null || engine == null || !engine.isRunning) {
            LiveScreen.Start
        } else {
            computeLiveScreen(
                phase = engine.currentPhase(),
                activeElapsedMs = engine.currentActiveElapsedMs(),
                isPaused = engine.isPaused,
                config = service.currentConfig(),
            )
        }
    }

    private fun unbindQuietly() {
        if (binder != null) {
            runCatching { getApplication<Application>().unbindService(connection) }
            binder = null
        }
    }

    private fun serviceIntent(): Intent = Intent(getApplication(), TimerService::class.java)

    override fun onCleared() {
        stopTicking()
        unbindQuietly()
        super.onCleared()
    }
}
