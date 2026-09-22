package com.example.skcttimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skcttimer.domain.ExamConfig
import com.example.skcttimer.domain.Segment
import com.example.skcttimer.domain.SegmentType
import com.example.skcttimer.ui.theme.Teal

/** §5 시험 화면: "진행 표시(영역 5칸 + 휴식 구간)" — 원 5개 + 그 사이 작은 막대(휴식)로 표현. */
@Composable
fun SectionProgressRow(config: ExamConfig, currentSegment: Segment) {
    val completedSections = when (currentSegment.type) {
        SegmentType.COUNTDOWN -> 0
        SegmentType.SECTION -> currentSegment.index - 1
        SegmentType.BREAK -> currentSegment.index
    }
    val currentSectionIndex = currentSegment.index.takeIf { currentSegment.type == SegmentType.SECTION }
    val currentBreakIndex = currentSegment.index.takeIf { currentSegment.type == SegmentType.BREAK }

    val doneColor = Teal
    val upcomingColor = MaterialTheme.colorScheme.outline

    Row {
        for (i in 1..config.sectionCount) {
            val sectionDone = i <= completedSections || i == currentSectionIndex
            SectionDot(filled = sectionDone, doneColor = doneColor, upcomingColor = upcomingColor)
            if (i < config.sectionCount) {
                Spacer(modifier = Modifier.width(4.dp))
                val breakDone = i <= completedSections || i == currentBreakIndex
                BreakTick(filled = breakDone, doneColor = doneColor, upcomingColor = upcomingColor)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun SectionDot(filled: Boolean, doneColor: Color, upcomingColor: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (filled) doneColor else upcomingColor),
    )
}

@Composable
private fun BreakTick(filled: Boolean, doneColor: Color, upcomingColor: Color) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .size(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (filled) doneColor else upcomingColor),
    )
}
