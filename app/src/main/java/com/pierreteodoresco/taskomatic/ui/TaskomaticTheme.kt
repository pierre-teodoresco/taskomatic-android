package com.pierreteodoresco.taskomatic.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF5757DC),
    secondary = Color(0xFF5757DC),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECEBFF),
    onSecondaryContainer = Color(0xFF202330),
    tertiary = Color(0xFF5757DC),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECEBFF),
    onTertiaryContainer = Color(0xFF202330),
    surfaceTint = Color(0xFF5757DC),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFEFF1F7),
    surfaceDim = Color(0xFFF7F8FC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECEBFF),
    onPrimaryContainer = Color(0xFF202330),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF1F7),
    surfaceContainerHighest = Color(0xFFEFF1F7),
    onSurface = Color(0xFF202330),
    onBackground = Color(0xFF202330),
    onSurfaceVariant = Color(0xFF686E82),
    outline = Color(0xFFE5E7EF),
    outlineVariant = Color(0xFFE5E7EF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9290FF),
    secondary = Color(0xFF9290FF),
    onSecondary = Color(0xFF101117),
    secondaryContainer = Color(0xFF2B2849),
    onSecondaryContainer = Color(0xFFF2F3FA),
    tertiary = Color(0xFF9290FF),
    onTertiary = Color(0xFF101117),
    tertiaryContainer = Color(0xFF2B2849),
    onTertiaryContainer = Color(0xFFF2F3FA),
    surfaceTint = Color(0xFF9290FF),
    surfaceContainerHigh = Color(0xFF1A1B24),
    surfaceBright = Color(0xFF242631),
    surfaceDim = Color(0xFF101117),
    onPrimary = Color(0xFF101117),
    primaryContainer = Color(0xFF2B2849),
    onPrimaryContainer = Color(0xFFF2F3FA),
    background = Color(0xFF101117),
    surface = Color(0xFF1A1B24),
    surfaceContainer = Color(0xFF1A1B24),
    surfaceContainerLow = Color(0xFF1A1B24),
    surfaceVariant = Color(0xFF242631),
    surfaceContainerHighest = Color(0xFF242631),
    onSurface = Color(0xFFF2F3FA),
    onBackground = Color(0xFFF2F3FA),
    onSurfaceVariant = Color(0xFFA2A8BC),
    outline = Color(0xFF323442),
    outlineVariant = Color(0xFF323442),
)

// Typography follows the iOS hierarchy while using Android's native font and font scaling.
private val TaskomaticTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineSmall = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun TaskomaticTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors,
        typography = TaskomaticTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(24.dp)), content = content)
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
