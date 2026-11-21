package dev.neonlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.data.NeonThemeConfig
import dev.neonlauncher.data.ThemeColors

/**
 * Resolved color and visual behavior tokens for the active Neon Launcher theme.
 */
@Immutable
data class NeonPalette(
    val themeId: String,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val surface: Color,
    val surfaceFocused: Color,
    val surfaceModal: Color,
    val surfaceBorder: Color,
    val focusedBorder: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val danger: Color,
    val highContrast: Boolean,
    val performanceMode: Boolean,
)

private val DefaultFallbackTheme = NeonThemeConfig(
    id = "cyber-cyan",
    name = "Cyber Cyan",
    nameEs = "Cian Cyber",
    author = "Neon Launcher",
    backgroundStyle = BackgroundStyle.AURORA,
    colors = ThemeColors(
        backgroundStart = 0xFF05060FL,
        backgroundEnd = 0xFF0B0F24L,
        surface = 0x14FFFFFFL,
        surfaceFocused = 0x26FFFFFFL,
        surfaceBorder = 0x4000F0FFL,
        accentPrimary = 0xFF00F0FFL,
        accentSecondary = 0xFFFF2BD6L,
        accentTertiary = 0xFF8A5CFFL,
        textPrimary = 0xFFF4F7FFL,
        textSecondary = 0xFFA8B3CFL,
        danger = 0xFFFF4D6DL,
    ),
)

val LocalNeonPalette = staticCompositionLocalOf {
    resolveNeonPalette(
        theme = DefaultFallbackTheme,
        highContrast = false,
        performanceMode = false,
    )
}

/**
 * Maps a [NeonThemeConfig] and accessibility/performance toggles into a [NeonPalette].
 */
fun resolveNeonPalette(
    theme: NeonThemeConfig?,
    highContrast: Boolean,
    performanceMode: Boolean,
): NeonPalette {
    val active = theme ?: DefaultFallbackTheme
    val c = active.colors
    val bgStart = Color(c.backgroundStart)
    val bgEnd = Color(c.backgroundEnd)
    val primary = Color(c.accentPrimary)
    val secondary = Color(c.accentSecondary)
    val tertiary = Color(c.accentTertiary)
    val baseSurface = Color(c.surface)
    val baseFocused = Color(c.surfaceFocused)
    val baseBorder = Color(c.surfaceBorder)
    val baseTextPrimary = Color(c.textPrimary)
    val baseTextSecondary = Color(c.textSecondary)
    val danger = Color(c.danger)

    return if (highContrast) {
        NeonPalette(
            themeId = active.id,
            backgroundStart = Color(0xFF020308L),
            backgroundEnd = lerp(bgEnd, Color.Black, 0.45f),
            surface = Color(0xD9101628L),
            surfaceFocused = Color(0xF01C2846L),
            surfaceModal = Color(0xFA090D1AL),
            surfaceBorder = primary.copy(alpha = 0.72f),
            focusedBorder = Color.White,
            accentPrimary = lerp(primary, Color.White, 0.15f),
            accentSecondary = lerp(secondary, Color.White, 0.15f),
            accentTertiary = lerp(tertiary, Color.White, 0.15f),
            textPrimary = Color.White,
            textSecondary = Color(0xFFE4EBFFL),
            danger = lerp(danger, Color.White, 0.12f),
            highContrast = true,
            performanceMode = performanceMode,
        )
    } else {
        NeonPalette(
            themeId = active.id,
            backgroundStart = bgStart,
            backgroundEnd = bgEnd,
            surface = baseSurface,
            surfaceFocused = baseFocused,
            surfaceModal = lerp(bgEnd, Color(0xFF060916L), 0.55f).copy(alpha = 0.94f),
            surfaceBorder = baseBorder,
            focusedBorder = primary,
            accentPrimary = primary,
            accentSecondary = secondary,
            accentTertiary = tertiary,
            textPrimary = baseTextPrimary,
            textSecondary = baseTextSecondary,
            danger = danger,
            highContrast = false,
            performanceMode = performanceMode,
        )
    }
}

/**
 * Provides both [LocalNeonPalette] and [androidx.tv.material3.MaterialTheme] for all TV screens.
 */
@Composable
fun NeonTheme(
    themeConfig: NeonThemeConfig?,
    highContrast: Boolean,
    performanceMode: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = resolveNeonPalette(
        theme = themeConfig,
        highContrast = highContrast,
        performanceMode = performanceMode,
    )
    val tvColorScheme = darkColorScheme(
        primary = palette.accentPrimary,
        onPrimary = palette.backgroundStart,
        secondary = palette.accentSecondary,
        onSecondary = palette.backgroundStart,
        tertiary = palette.accentTertiary,
        onTertiary = palette.backgroundStart,
        background = palette.backgroundStart,
        onBackground = palette.textPrimary,
        surface = palette.surfaceModal,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surface,
        onSurfaceVariant = palette.textSecondary,
        error = palette.danger,
        onError = Color.White,
        border = palette.surfaceBorder,
    )

    CompositionLocalProvider(LocalNeonPalette provides palette) {
        MaterialTheme(
            colorScheme = tvColorScheme,
            typography = NeonTypography.tvTypography,
            content = content,
        )
    }
}
