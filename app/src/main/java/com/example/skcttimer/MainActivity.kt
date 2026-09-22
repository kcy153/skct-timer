package com.example.skcttimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.content.ContextCompat
import com.example.skcttimer.audio.Haptics
import com.example.skcttimer.audio.ToneGenerator
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.LiveScreen
import com.example.skcttimer.ui.TimerViewModel
import com.example.skcttimer.ui.screens.BreakScreen
import com.example.skcttimer.ui.screens.ExamScreen
import com.example.skcttimer.ui.screens.FinishedScreen
import com.example.skcttimer.ui.screens.SettingsScreen
import com.example.skcttimer.ui.screens.StartScreen
import com.example.skcttimer.ui.theme.SkctTimerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()

    // §8-4: 거부해도 앱은 동작해야 하고, 안내를 한 번만 보여준다(재설치 전까지 다시 안 뜸).
    private val showNotificationNotice: MutableState<Boolean> = mutableStateOf(false)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && !viewModel.settings.notificationPermissionNoticeShown) {
                viewModel.settings.notificationPermissionNoticeShown = true
                showNotificationNotice.value = true
            }
        }

    private lateinit var previewTones: ToneGenerator
    private lateinit var previewHaptics: Haptics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        previewTones = ToneGenerator().also { it.prepare() }
        previewHaptics = Haptics(this)

        setContent {
            SkctTimerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    override fun onDestroy() {
        previewTones.release()
        super.onDestroy()
    }

    @Composable
    private fun AppRoot() {
        var showSettings by remember { mutableStateOf(false) }
        var showAbortConfirm by remember { mutableStateOf(false) }
        val live by viewModel.liveScreen.collectAsState()

        // SettingsRepository는 SharedPreferences 기반 평범한 프로퍼티라 값이 바뀌어도 Compose가
        // 자동으로 다시 그리지 않는다 — 여기 Compose 상태로 한 번 더 들고 있다가 변경 시
        // 저장소와 함께(write-through) 갱신해야 화면이 즉시 반영된다.
        var breakSec by remember { mutableStateOf(viewModel.settings.breakSec) }
        var soundEnabled by remember { mutableStateOf(viewModel.settings.soundEnabled) }
        var vibrationEnabled by remember { mutableStateOf(viewModel.settings.vibrationEnabled) }

        // §5: 시험 중(Exam/Break)에는 화면이 꺼지지 않게 한다. 그 외 화면은 평소대로 둔다.
        val keepScreenOn = live is LiveScreen.Exam || live is LiveScreen.BreakTime
        LaunchedEffect(keepScreenOn) {
            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        // §5: 시험 중 뒤로가기 -> 중단 확인창. 설정 화면 중 뒤로가기 -> 닫기.
        BackHandler(enabled = showSettings) { showSettings = false }
        BackHandler(enabled = !showSettings && (live is LiveScreen.Exam || live is LiveScreen.BreakTime)) {
            showAbortConfirm = true
        }

        if (showNotificationNotice.value) {
            AlertDialog(
                onDismissRequest = { showNotificationNotice.value = false },
                title = { Text("알림 권한이 꺼져 있어요") },
                text = {
                    Text(
                        "진행 상태를 보여주는 알림이 안 뜰 수 있어요. 예비음·경계음·진동은 이 설정과 " +
                            "무관하게 그대로 울려요. 나중에 기기 설정에서 알림 권한을 다시 켤 수 있어요.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showNotificationNotice.value = false }) { Text("확인") }
                },
            )
        }

        if (showAbortConfirm) {
            AlertDialog(
                onDismissRequest = { showAbortConfirm = false },
                title = { Text("시험을 중단할까요?") },
                confirmButton = {
                    TextButton(onClick = {
                        showAbortConfirm = false
                        viewModel.stopAndReturnToStart()
                    }) { Text("중단") }
                },
                dismissButton = {
                    TextButton(onClick = { showAbortConfirm = false }) { Text("계속하기") }
                },
            )
        }

        when {
            showSettings -> SettingsScreen(
                breakSec = breakSec,
                onBreakSecChange = {
                    breakSec = it
                    viewModel.settings.breakSec = it
                },
                soundEnabled = soundEnabled,
                onSoundToggle = {
                    soundEnabled = it
                    viewModel.settings.soundEnabled = it
                },
                vibrationEnabled = vibrationEnabled,
                vibrationSupported = previewHaptics.isSupported,
                onVibrationToggle = {
                    vibrationEnabled = it
                    viewModel.settings.vibrationEnabled = it
                },
                onPreview = { kind ->
                    previewTones.play(kind)
                    previewHaptics.vibrate(kind)
                },
                onClose = { showSettings = false },
            )

            else -> when (val screen = live) {
                LiveScreen.Start -> StartScreen(
                    breakSec = breakSec,
                    onStart = { viewModel.startExam() },
                    onOpenSettings = { showSettings = true },
                    debugFastStart = if (BuildConfig.DEBUG) {
                        { viewModel.startExam(debugScaleDivisor = 60) }
                    } else {
                        null
                    },
                )

                is LiveScreen.Exam -> ExamScreen(
                    screen = screen,
                    // breakSec은 세그먼트 이름 조회(segmentLabel)에 영향 없어 현재 설정값으로 충분 —
                    // 어차피 시험 진행 중엔 설정 화면 자체가 접근 불가라 실제 실행 중인 값과 항상 같다.
                    config = ExamConfig(breakSec = breakSec),
                    onPauseResume = { if (screen.paused) viewModel.resume() else viewModel.pause() },
                )

                is LiveScreen.BreakTime -> BreakScreen(
                    screen = screen,
                    onPauseResume = { if (screen.paused) viewModel.resume() else viewModel.pause() },
                )

                LiveScreen.Finished -> FinishedScreen(
                    onRestart = { viewModel.stopAndReturnToStart() },
                )
            }
        }
    }
}
