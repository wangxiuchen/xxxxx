package com.example.appopencounter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CounterBlue,
    onPrimary = Color.White,
    background = CounterMist,
    onBackground = CounterInk,
    surface = Color.White,
    onSurface = CounterInk,
)

@Composable
fun AppOpenCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}
