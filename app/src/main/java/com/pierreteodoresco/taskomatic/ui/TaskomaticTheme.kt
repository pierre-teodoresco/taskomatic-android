package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF5653CD), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E7FF), onPrimaryContainer = Color(0xFF38358B),
    background = Color(0xFFF8F8FC), surface = Color(0xFFF8F8FC),
    surfaceContainer = Color.White, surfaceContainerLow = Color.White,
    onSurface = Color(0xFF242538), onSurfaceVariant = Color(0xFF646579),
    outlineVariant = Color(0xFFE2E2ED),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFC3BEFF), onPrimary = Color(0xFF302977),
    primaryContainer = Color(0xFF3C3785), onPrimaryContainer = Color(0xFFE7E3FF),
    background = Color(0xFF14141D), surface = Color(0xFF14141D),
    surfaceContainer = Color(0xFF23232F), surfaceContainerLow = Color(0xFF1E1E29),
    onSurface = Color(0xFFE9E8F2), onSurfaceVariant = Color(0xFFB5B3C7),
    outlineVariant = Color(0xFF3B3A4C),
)

@Composable
fun TaskomaticTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

@Composable
fun DialogSystemBarAppearance() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SideEffect {
        window?.let { WindowCompat.getInsetsController(it, it.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        } }
    }
}
