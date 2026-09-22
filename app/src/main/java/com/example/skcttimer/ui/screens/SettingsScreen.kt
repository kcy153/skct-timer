package com.example.skcttimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skcttimer.domain.AlertKind
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.formatMmSs
import kotlin.math.roundToInt

/** §5 화면 5번 / §5 설정 화면 세부. */
@Composable
fun SettingsScreen(
    breakSec: Int,
    onBreakSecChange: (Int) -> Unit,
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    vibrationEnabled: Boolean,
    vibrationSupported: Boolean,
    onVibrationToggle: (Boolean) -> Unit,
    onPreview: (AlertKind) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineSmall)

        Column {
            Text("영역 사이 휴식 시간: ${formatMmSs(breakSec)}")
            Slider(
                value = breakSec.toFloat(),
                onValueChange = { onBreakSecChange(((it / 10f).roundToInt() * 10)) },
                valueRange = ExamConfig.MIN_BREAK_SEC.toFloat()..ExamConfig.MAX_BREAK_SEC.toFloat(),
                steps = (ExamConfig.MAX_BREAK_SEC - ExamConfig.MIN_BREAK_SEC) / ExamConfig.BREAK_STEP_SEC - 1,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("소리")
            Switch(checked = soundEnabled, onCheckedChange = onSoundToggle)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (vibrationSupported) "진동" else "진동(이 기기는 지원하지 않아요)")
            Switch(checked = vibrationEnabled, enabled = vibrationSupported, onCheckedChange = onVibrationToggle)
        }

        Column {
            Text("미리듣기", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onPreview(AlertKind.PRE) }) { Text("예비음") }
                TextButton(onClick = { onPreview(AlertKind.MID) }) { Text("3분전음") }
                TextButton(onClick = { onPreview(AlertKind.BOUNDARY) }) { Text("경계음") }
            }
        }

        Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("닫기") }
    }
}
