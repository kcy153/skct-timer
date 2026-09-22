package com.example.skcttimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skcttimer.domain.LiveScreen
import com.example.skcttimer.domain.formatMmSs
import com.example.skcttimer.ui.theme.TabularNumsStyle
import com.example.skcttimer.ui.theme.Teal

/**
 * §5 화면 3번: 휴식 화면. "색만으로 상태를 구분하지 않는다"(§6) — 그래서 "휴식" 문구를 항상 넣는다.
 * 시험 화면과 구분되는 차분한 톤(§5)으로 강조색은 Teal(경고색 Amber를 쓰지 않음).
 */
@Composable
fun BreakScreen(
    screen: LiveScreen.BreakTime,
    onPauseResume: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("휴식", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Teal)

        Text(
            text = formatMmSs(screen.remainingSec),
            fontSize = 72.sp,
            textAlign = TextAlign.Center,
            color = Teal,
            style = TabularNumsStyle,
        )

        Text("다음: ${screen.nextSectionName}", style = MaterialTheme.typography.titleMedium)

        if (screen.paused) {
            Text("일시정지됨", color = MaterialTheme.colorScheme.onBackground)
        }

        Button(onClick = onPauseResume) {
            Text(if (screen.paused) "재개" else "일시정지")
        }
    }
}
