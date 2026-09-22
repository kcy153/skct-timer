package com.example.skcttimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.buildTimeline
import kotlin.math.roundToInt

/** §5 화면 1번: 시작 화면. */
@Composable
fun StartScreen(
    breakSec: Int,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    debugFastStart: (() -> Unit)? = null,
) {
    val config = remember(breakSec) { ExamConfig(breakSec = breakSec) }
    val totalMinutes = remember(breakSec) {
        (buildTimeline(config).maxOf { it.endSec } / 60.0).roundToInt()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("쓱시티 타이머", style = MaterialTheme.typography.headlineMedium)
        Text("영역 ${config.sectionCount}개 · 각 ${config.sectionSec / 60}분 · 영역 사이 휴식 ${breakSec}초")
        Text("총 소요시간 약 ${totalMinutes}분", style = MaterialTheme.typography.titleMedium)

        Button(onClick = onStart) { Text("시작") }
        TextButton(onClick = onOpenSettings) { Text("설정") }

        Text(
            "정확한 시각에 알림이 울리도록 배터리 최적화 제외를 권장해요",
            style = MaterialTheme.typography.bodySmall,
        )

        if (debugFastStart != null) {
            TextButton(onClick = debugFastStart) { Text("빠른 테스트 시작 (1/60 배율)") }
        }
    }
}
