package com.maximx1.fads.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors =
    darkColorScheme(
        primary = Color(0xFFFFE082),
        onPrimary = Color(0xFF1B1B1B),
    )

@Composable
fun FadsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
