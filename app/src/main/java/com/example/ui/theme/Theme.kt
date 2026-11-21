package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.feature.settings.AccentColorOption
import com.example.feature.settings.DisplayDensity
import com.example.feature.settings.FontSizePreset
import com.example.feature.settings.ThemeMode

data class XtraDisplayConfig(
    val isDark: Boolean = true,
    val accentColor: Color = XBlue,
    val spacingScale: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false
)

val LocalXtraDisplayConfig = staticCompositionLocalOf { XtraDisplayConfig() }

private fun buildDarkScheme(accent: Color, highContrast: Boolean): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.16f),
    onPrimaryContainer = accent,
    secondary = GoldOrg,
    onSecondary = OledBlack,
    secondaryContainer = GoldOrgContainer,
    onSecondaryContainer = GoldOrg,
    tertiary = EmeraldAdShield,
    onTertiary = OledBlack,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldAdShield,
    background = OledBlack,
    onBackground = if (highContrast) Color.White else TextPrimary,
    surface = OledBlack,
    onSurface = if (highContrast) Color.White else TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = if (highContrast) TextSecondary else TextMuted,
    surfaceContainerLowest = OledBlack,
    surfaceContainerLow = SurfaceSubtle,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceHover,
    outline = if (highContrast) Color(0xFF53595E) else HairlineDivider,
    outlineVariant = BorderSubtle,
    error = DangerUndo,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = DangerUndo
)

private fun buildLightScheme(accent: Color, highContrast: Boolean): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.12f),
    onPrimaryContainer = accent,
    secondary = Color(0xFFB58E07),
    onSecondary = Color.White,
    secondaryContainer = GoldOrgContainer,
    onSecondaryContainer = Color(0xFF7A5F00),
    tertiary = Color(0xFF008A5B),
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFF006B47),
    background = LightCanvas,
    onBackground = if (highContrast) Color.Black else LightTextPrimary,
    surface = LightCanvas,
    onSurface = if (highContrast) Color.Black else LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = if (highContrast) LightTextPrimary else LightTextMuted,
    surfaceContainerLowest = LightCanvas,
    surfaceContainerLow = LightSurfaceSubtle,
    surfaceContainer = LightSurfaceCard,
    surfaceContainerHigh = LightSurfaceElevated,
    surfaceContainerHighest = LightSurfaceHover,
    outline = if (highContrast) Color(0xFF8B98A5) else LightHairlineDivider,
    outlineVariant = LightBorderSubtle,
    error = DangerUndo,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = DangerUndo
)

private val XProShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun Typography.scaled(factor: Float): Typography {
    if (factor == 1.0f) return this
    fun TextStyle.s() = copy(
        fontSize = fontSize * factor,
        lineHeight = lineHeight * factor
    )
    return copy(
        displayLarge = displayLarge.s(),
        displayMedium = displayMedium.s(),
        displaySmall = displaySmall.s(),
        headlineLarge = headlineLarge.s(),
        headlineMedium = headlineMedium.s(),
        headlineSmall = headlineSmall.s(),
        titleLarge = titleLarge.s(),
        titleMedium = titleMedium.s(),
        titleSmall = titleSmall.s(),
        bodyLarge = bodyLarge.s(),
        bodyMedium = bodyMedium.s(),
        bodySmall = bodySmall.s(),
        labelLarge = labelLarge.s(),
        labelMedium = labelMedium.s(),
        labelSmall = labelSmall.s()
    )
}

@Composable
fun XProTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentOption: AccentColorOption = AccentColorOption.X_BLUE,
    fontSizePreset: FontSizePreset = FontSizePreset.MEDIUM,
    displayDensity: DisplayDensity = DisplayDensity.STANDARD,
    largerText: Boolean = false,
    reducedMotion: Boolean = false,
    improvedContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val accent = Color(accentOption.hex)
    val scheme = if (useDark) {
        buildDarkScheme(accent, improvedContrast)
    } else {
        buildLightScheme(accent, improvedContrast)
    }
    val effectiveScale = fontSizePreset.scale * (if (largerText) 1.10f else 1.0f)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    CompositionLocalProvider(
        LocalXtraDisplayConfig provides XtraDisplayConfig(
            isDark = useDark,
            accentColor = accent,
            spacingScale = displayDensity.spacingScale,
            reducedMotion = reducedMotion,
            highContrast = improvedContrast
        )
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = XProTypography.scaled(effectiveScale),
            shapes = XProShapes,
            content = content
        )
    }
}
