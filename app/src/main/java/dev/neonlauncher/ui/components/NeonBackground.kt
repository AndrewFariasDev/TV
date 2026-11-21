package dev.neonlauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.ui.theme.LocalNeonPalette

/**
 * Full-screen adaptive Neon backdrop supporting [BackgroundStyle.AURORA],
 * [BackgroundStyle.GRID], and [BackgroundStyle.SOLID], gracefully scaling down
 * animations and blur when [performanceMode] is enabled.
 */
@Composable
fun NeonBackground(
    style: BackgroundStyle,
    performanceMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val drift = if (!performanceMode && style == BackgroundStyle.AURORA) {
        val transition = rememberInfiniteTransition(label = "auroraDrift")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "auroraPhase",
        )
        progress
    } else {
        0.35f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(palette.backgroundStart, palette.backgroundEnd),
                ),
            ),
    ) {
        when (style) {
            BackgroundStyle.AURORA -> {
                AuroraLayer(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    drift = drift,
                    highContrast = palette.highContrast,
                    performanceMode = performanceMode,
                )
            }

            BackgroundStyle.GRID -> {
                AuroraLayer(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    drift = 0.25f,
                    highContrast = palette.highContrast,
                    performanceMode = true,
                    intensityMultiplier = 0.55f,
                )
                CyberPerspectiveGrid(
                    lineColor = palette.accentPrimary.copy(
                        alpha = if (palette.highContrast) 0.16f else 0.12f,
                    ),
                    horizonGlowColor = palette.accentSecondary.copy(
                        alpha = if (palette.highContrast) 0.14f else 0.16f,
                    ),
                )
            }

            BackgroundStyle.SOLID -> {
                // Base dark contrast gradient only; ideal for ultra-low RAM or pure minimalism.
            }
        }
    }
}

@Composable
private fun AuroraLayer(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    drift: Float,
    highContrast: Boolean,
    performanceMode: Boolean,
    intensityMultiplier: Float = 1f,
) {
    val alphaBase = (if (highContrast) 0.10f else 0.18f) * intensityMultiplier
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .neonAmbientBlur(radius = 32.dp, performanceMode = performanceMode),
    ) {
        val w = size.width
        val h = size.height

        val orb1Center = Offset(
            x = w * (0.16f + drift * 0.10f),
            y = h * (0.20f + drift * 0.06f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = alphaBase), Color.Transparent),
                center = orb1Center,
                radius = w * 0.45f,
            ),
            radius = w * 0.45f,
            center = orb1Center,
        )

        val orb2Center = Offset(
            x = w * (0.84f - drift * 0.08f),
            y = h * (0.78f - drift * 0.08f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = alphaBase * 0.85f), Color.Transparent),
                center = orb2Center,
                radius = w * 0.48f,
            ),
            radius = w * 0.48f,
            center = orb2Center,
        )

        val orb3Center = Offset(
            x = w * 0.52f,
            y = h * (0.38f + drift * 0.05f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = alphaBase * 0.65f), Color.Transparent),
                center = orb3Center,
                radius = w * 0.36f,
            ),
            radius = w * 0.36f,
            center = orb3Center,
        )
    }
}

@Composable
private fun CyberPerspectiveGrid(
    lineColor: Color,
    horizonGlowColor: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.58f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, horizonGlowColor, Color.Transparent),
                startY = horizonY - h * 0.12f,
                endY = horizonY + h * 0.18f,
            ),
        )

        // Horizontal perspective lines accelerating toward the viewer
        val horizontalSteps = 8
        for (i in 0..horizontalSteps) {
            val t = i / horizontalSteps.toFloat()
            val y = horizonY + (h - horizonY) * (t * t)
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f,
            )
        }

        // Vanishing-point perspective rays
        val vanishingPoint = Offset(w * 0.5f, horizonY)
        val rayCount = 9
        for (i in -rayCount..rayCount) {
            val bottomX = w * 0.5f + i * (w * 0.13f)
            drawLine(
                color = lineColor,
                start = vanishingPoint,
                end = Offset(bottomX, h),
                strokeWidth = 1.1f,
            )
        }
    }
}
