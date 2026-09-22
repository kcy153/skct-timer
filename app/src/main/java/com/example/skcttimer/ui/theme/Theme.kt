package com.example.skcttimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** 다크 모드 전용 보조면 색상 — 계획서 §6엔 명시 없어 Navy를 살짝 밝힌 값으로 도출. */
private val NavySurface = Color(0xFF16243A)

private val LightColors = lightColorScheme(
    background = Paper,
    onBackground = Navy,
    surface = Ice,
    onSurface = Navy,
    surfaceVariant = Ice,
    onSurfaceVariant = Navy,
    primary = Teal,
    onPrimary = Ice,
    secondary = Teal,
    onSecondary = Ice,
    outline = Navy.copy(alpha = 0.3f),
)

private val DarkColors = darkColorScheme(
    background = Navy,
    onBackground = Ice,
    surface = NavySurface,
    onSurface = Ice,
    surfaceVariant = NavySurface,
    onSurfaceVariant = Ice,
    primary = Teal,
    onPrimary = Navy,
    secondary = Teal,
    onSecondary = Navy,
    outline = Ice.copy(alpha = 0.3f),
)

/** 남은 시간처럼 자릿수가 바뀌어도 숫자 폭이 흔들리면 안 되는 곳에 적용(§6, tabular numerals). */
val TabularNumsStyle = TextStyle(fontFeatureSettings = "tnum")

private val AppTypography = Typography()

@Composable
fun SkctTimerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}

/** §6: 마지막 3분 경고에만 쓰는 색 — 다크/라이트 공통이라 ColorScheme에 안 넣고 상수로 노출. */
val WarningAmber = Amber
