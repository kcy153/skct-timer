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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.LiveScreen
import com.example.skcttimer.domain.SegmentType
import com.example.skcttimer.domain.formatMmSs
import com.example.skcttimer.domain.segmentLabel
import com.example.skcttimer.ui.components.SectionProgressRow
import com.example.skcttimer.ui.theme.TabularNumsStyle
import com.example.skcttimer.ui.theme.WarningAmber

/** §5 화면 2번: 시험 화면. 카운트다운도 같은 화면으로 그린다(§4-1). */
@Composable
fun ExamScreen(
    screen: LiveScreen.Exam,
    config: ExamConfig,
    onPauseResume: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(segmentLabel(config, screen.segment), style = MaterialTheme.typography.titleMedium)

        Text(
            text = formatMmSs(screen.remainingSec),
            fontSize = 88.sp,
            textAlign = TextAlign.Center,
            color = if (screen.isWarning) WarningAmber else MaterialTheme.colorScheme.onBackground,
            style = TabularNumsStyle,
        )

        if (screen.segment.type == SegmentType.SECTION) {
            SectionProgressRow(config, screen.segment)
        }

        if (screen.paused) {
            Text("일시정지됨", color = MaterialTheme.colorScheme.onBackground)
        }

        Button(onClick = onPauseResume) {
            Text(if (screen.paused) "재개" else "일시정지")
        }
    }
}
