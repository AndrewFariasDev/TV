package dev.neonlauncher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.ui.theme.LocalNeonPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen adaptive Neon backdrop supporting [BackgroundStyle.AURORA],
 * [BackgroundStyle.GRID], [BackgroundStyle.CYBER_PULSE], and [BackgroundStyle.SOLID].
 */
@Composable
fun NeonBackground(
    style: BackgroundStyle,
    performanceMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current

    val transition = rememberInfiniteTransition(label = "neonBackdropLoop")
    val auroraDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auroraDrift",
    )
    val flowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flowPhase",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
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
                    drift = auroraDrift,
                    highContrast = palette.highContrast,
                    performanceMode = performanceMode,
                )
            }

            BackgroundStyle.GRID -> {
                AuroraLayer(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    drift = auroraDrift,
                    highContrast = palette.highContrast,
                    performanceMode = true,
                    intensityMultiplier = 0.65f,
                )
                CyberPerspectiveGrid(
                    lineColor = palette.accentPrimary.copy(
                        alpha = if (palette.highContrast) 0.24f else 0.20f,
                    ),
                    horizonGlowColor = palette.accentSecondary.copy(
                        alpha = if (palette.highContrast) 0.20f else 0.24f,
                    ),
                    scrollPhase = flowPhase,
                )
            }

            BackgroundStyle.CYBER_PULSE -> {
                CyberPulseAnimatedBackdrop(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    scrollPhase = flowPhase,
                    pulseWave = auroraDrift,
                    highContrast = palette.highContrast,
                )
            }

            BackgroundStyle.SOLID -> {
                // Static high-contrast dark gradient only.
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
    val alphaBase = (if (highContrast) 0.14f else 0.24f) * intensityMultiplier
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .neonAmbientBlur(radius = 32.dp, performanceMode = performanceMode),
    ) {
        val w = size.width
        val h = size.height

        val orb1Center = Offset(
            x = w * (0.14f + drift * 0.22f),
            y = h * (0.18f + drift * 0.14f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = alphaBase), Color.Transparent),
                center = orb1Center,
                radius = w * 0.46f,
            ),
            radius = w * 0.46f,
            center = orb1Center,
        )

        val orb2Center = Offset(
            x = w * (0.86f - drift * 0.22f),
            y = h * (0.82f - drift * 0.16f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = alphaBase * 0.9f), Color.Transparent),
                center = orb2Center,
                radius = w * 0.50f,
            ),
            radius = w * 0.50f,
            center = orb2Center,
        )

        val orb3Center = Offset(
            x = w * (0.45f + sin(drift * PI).toFloat() * 0.12f),
            y = h * (0.34f + drift * 0.12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = alphaBase * 0.75f), Color.Transparent),
                center = orb3Center,
                radius = w * 0.38f,
            ),
            radius = w * 0.38f,
            center = orb3Center,
        )
    }
}

@Composable
private fun CyberPerspectiveGrid(
    lineColor: Color,
    horizonGlowColor: Color,
    scrollPhase: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.56f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, horizonGlowColor, Color.Transparent),
                startY = horizonY - h * 0.14f,
                endY = horizonY + h * 0.20f,
            ),
        )

        // Scrolling horizontal perspective lines moving toward the viewer
        val horizontalSteps = 9
        for (i in 0 until horizontalSteps) {
            val t = ((i + scrollPhase) / horizontalSteps.toFloat()).coerceIn(0f, 1f)
            val y = horizonY + (h - horizonY) * (t * t)
            val alphaBoost = (0.3f + 0.7f * t).coerceIn(0f, 1f)
            drawLine(
                color = lineColor.copy(alpha = lineColor.alpha * alphaBoost),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.1f + (1.4f * t),
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
                strokeWidth = 1.2f,
            )
        }
    }
}

/**
 * High-impact animated futuristic backdrop designed for Red & Black (`crimson-pulse`) and
 * dark cyber themes: scrolling 3D laser grid, expanding reactor pulse rings, sweeping
 * holographic scanline, and rising neon cyber embers.
 */
@Composable
private fun CyberPulseAnimatedBackdrop(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    scrollPhase: Float,
    pulseWave: Float,
    highContrast: Boolean,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.54f
        val coreCenter = Offset(w * 0.5f, horizonY)

        // 1. Pulsing crimson/neon core glow at horizon
        val coreRadius = w * (0.36f + pulseWave * 0.10f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = if (highContrast) 0.22f else 0.30f),
                    tertiary.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = coreCenter,
                radius = coreRadius,
            ),
            radius = coreRadius,
            center = coreCenter,
        )

        // 2. Expanding concentric cyber reactor rings
        for (ringIdx in 0..2) {
            val ringProgress = (scrollPhase + ringIdx / 3f) % 1f
            val ringRadius = (w * 0.06f) + (w * 0.38f * ringProgress)
            val ringAlpha = (1f - ringProgress) * (if (highContrast) 0.28f else 0.36f)
            drawCircle(
                color = primary.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = coreCenter,
                style = Stroke(width = 1.8f + (1f - ringProgress) * 1.5f),
            )
        }

        // 3. Scrolling 3D perspective laser grid
        val horizontalSteps = 10
        for (i in 0 until horizontalSteps) {
            val t = ((i + scrollPhase) / horizontalSteps.toFloat()).coerceIn(0f, 1f)
            val y = horizonY + (h - horizonY) * (t * t)
            val lineAlpha = (0.12f + 0.28f * t) * (if (highContrast) 0.85f else 1f)
            drawLine(
                color = primary.copy(alpha = lineAlpha),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f + (1.8f * t),
            )
        }

        val rayCount = 10
        for (i in -rayCount..rayCount) {
            val bottomX = w * 0.5f + i * (w * 0.125f)
            drawLine(
                color = primary.copy(alpha = 0.20f),
                start = coreCenter,
                end = Offset(bottomX, h),
                strokeWidth = 1.2f,
            )
        }

        // 4. Horizon laser bar
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    secondary.copy(alpha = 0.65f),
                    primary.copy(alpha = 0.90f),
                    secondary.copy(alpha = 0.65f),
                    Color.Transparent,
                ),
            ),
            start = Offset(w * 0.05f, horizonY),
            end = Offset(w * 0.95f, horizonY),
            strokeWidth = 2.4f,
        )

        // 5. Sweeping horizontal HUD scanline
        val scanY = h * scrollPhase
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    primary.copy(alpha = 0.10f),
                    secondary.copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                startY = (scanY - 28f).coerceAtLeast(0f),
                endY = (scanY + 28f).coerceAtMost(h),
            ),
        )

        // 6. Rising neon cyber embers / data nodes
        val nodeCount = 14
        for (idx in 0 until nodeCount) {
            val seed = (idx * 0.618034f) % 1f
            val nodeProgress = (scrollPhase + seed) % 1f
            val xBase = w * ((idx + 0.5f) / nodeCount.toFloat())
            val xWobble = cos((nodeProgress * 2f * PI + idx).toDouble()).toFloat() * 24f
            val x = (xBase + xWobble).coerceIn(0f, w)
            val y = h * (1f - nodeProgress)
            val nodeAlpha = sin(nodeProgress * PI).toFloat().coerceIn(0f, 1f) * 0.55f
            val radius = 2.2f + (idx % 3) * 1.4f
            drawCircle(
                color = if (idx % 2 == 0) primary.copy(alpha = nodeAlpha) else secondary.copy(alpha = nodeAlpha),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}
