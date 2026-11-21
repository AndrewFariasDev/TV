package dev.neonlauncher.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.ui.theme.LocalNeonPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Full-screen real-time animated Neon backdrop supporting [BackgroundStyle.AURORA],
 * [BackgroundStyle.GRID], [BackgroundStyle.CYBER_PULSE], and [BackgroundStyle.SOLID].
 *
 * Uses wall-clock [SystemClock.elapsedRealtime] read directly inside `DrawScope`
 * so animations run smoothly at ~60 FPS with zero recompositions and stay 100%
 * functional even when `Settings.Global.ANIMATOR_DURATION_SCALE` is set to 0 on a TV.
 */
@Composable
fun NeonBackground(
    style: BackgroundStyle,
    performanceMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val timeSecondsState = remember(style) { mutableFloatStateOf(0f) }

    LaunchedEffect(style) {
        if (style == BackgroundStyle.SOLID) return@LaunchedEffect
        val startMs = SystemClock.elapsedRealtime()
        while (isActive) {
            timeSecondsState.floatValue = (SystemClock.elapsedRealtime() - startMs) / 1000f
            delay(16L)
        }
    }

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
                AuroraAnimatedLayer(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    timeSecondsState = timeSecondsState,
                    highContrast = palette.highContrast,
                    performanceMode = performanceMode,
                )
            }

            BackgroundStyle.GRID -> {
                AuroraAnimatedLayer(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    timeSecondsState = timeSecondsState,
                    highContrast = palette.highContrast,
                    performanceMode = true,
                    intensityMultiplier = 0.65f,
                )
                CyberPerspectiveGrid(
                    lineColor = palette.accentPrimary.copy(
                        alpha = if (palette.highContrast) 0.28f else 0.24f,
                    ),
                    horizonGlowColor = palette.accentSecondary.copy(
                        alpha = if (palette.highContrast) 0.24f else 0.28f,
                    ),
                    timeSecondsState = timeSecondsState,
                )
            }

            BackgroundStyle.CYBER_PULSE -> {
                CyberPulseAnimatedBackdrop(
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                    tertiary = palette.accentTertiary,
                    timeSecondsState = timeSecondsState,
                    highContrast = palette.highContrast,
                )
            }

            BackgroundStyle.SOLID -> Unit
        }
    }
}

@Composable
private fun AuroraAnimatedLayer(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    timeSecondsState: FloatState,
    highContrast: Boolean,
    performanceMode: Boolean,
    intensityMultiplier: Float = 1f,
) {
    val alphaBase = (if (highContrast) 0.16f else 0.28f) * intensityMultiplier
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .neonAmbientBlur(radius = 32.dp, performanceMode = performanceMode),
    ) {
        val timeSeconds = timeSecondsState.floatValue
        val w = size.width
        val h = size.height

        val orb1X = w * (0.24f + cos(timeSeconds * 0.65).toFloat() * 0.18f)
        val orb1Y = h * (0.26f + sin(timeSeconds * 0.50).toFloat() * 0.14f)
        val orb1Center = Offset(orb1X, orb1Y)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = alphaBase), Color.Transparent),
                center = orb1Center,
                radius = w * 0.46f,
            ),
            radius = w * 0.46f,
            center = orb1Center,
        )

        val orb2X = w * (0.76f + sin(timeSeconds * 0.55).toFloat() * 0.18f)
        val orb2Y = h * (0.72f + cos(timeSeconds * 0.45).toFloat() * 0.15f)
        val orb2Center = Offset(orb2X, orb2Y)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = alphaBase * 0.92f), Color.Transparent),
                center = orb2Center,
                radius = w * 0.50f,
            ),
            radius = w * 0.50f,
            center = orb2Center,
        )

        val orb3X = w * (0.50f + sin(timeSeconds * 0.80).toFloat() * 0.20f)
        val orb3Y = h * (0.42f + cos(timeSeconds * 0.70).toFloat() * 0.14f)
        val orb3Center = Offset(orb3X, orb3Y)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = alphaBase * 0.80f), Color.Transparent),
                center = orb3Center,
                radius = w * 0.38f,
            ),
            radius = w * 0.38f,
            center = orb3Center,
        )

        // Subtle drifting luminous particles for visible motion on Aurora
        val particleCount = 12
        for (idx in 0 until particleCount) {
            val seed = (idx * 0.618034f) % 1f
            val progress = ((timeSeconds * (0.09f + (idx % 3) * 0.03f)) + seed) % 1f
            val x = (w * ((idx + 0.5f) / particleCount.toFloat()) +
                cos(timeSeconds * 1.1 + idx).toFloat() * 28f).coerceIn(0f, w)
            val y = h * (1f - progress)
            val particleAlpha = sin(progress * PI).toFloat().coerceIn(0f, 1f) * 0.48f * intensityMultiplier
            drawCircle(
                color = if (idx % 2 == 0) primary.copy(alpha = particleAlpha) else secondary.copy(alpha = particleAlpha),
                radius = 2.0f + (idx % 3) * 1.2f,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun CyberPerspectiveGrid(
    lineColor: Color,
    horizonGlowColor: Color,
    timeSecondsState: FloatState,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val timeSeconds = timeSecondsState.floatValue
        val scrollPhase = (timeSeconds * 0.45f) % 1f
        val pulse = (sin(timeSeconds * 2.2).toFloat() + 1f) * 0.5f

        val w = size.width
        val h = size.height
        val horizonY = h * 0.55f

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    horizonGlowColor.copy(alpha = horizonGlowColor.alpha * (0.75f + 0.25f * pulse)),
                    Color.Transparent,
                ),
                startY = horizonY - h * 0.15f,
                endY = horizonY + h * 0.22f,
            ),
        )

        val horizontalSteps = 10
        for (i in 0 until horizontalSteps) {
            val t = ((i + scrollPhase) / horizontalSteps.toFloat()).coerceIn(0f, 1f)
            val y = horizonY + (h - horizonY) * (t * t)
            val alphaBoost = (0.25f + 0.75f * t).coerceIn(0f, 1f)
            drawLine(
                color = lineColor.copy(alpha = lineColor.alpha * alphaBoost),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f + (1.8f * t),
            )
        }

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
 * Real-time animated futuristic cyber backdrop featuring:
 * - Counter-rotating HUD reactor arc rings at the center
 * - Expanding shockwave energy rings
 * - Continuously advancing 3D perspective laser grid
 * - Laser data pulses traveling down perspective rays
 * - Sweeping holographic scanline and floating neon embers
 */
@Composable
private fun CyberPulseAnimatedBackdrop(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    timeSecondsState: FloatState,
    highContrast: Boolean,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val timeSeconds = timeSecondsState.floatValue
        val gridPhase = (timeSeconds * 0.55f) % 1f
        val pulseWave = (sin(timeSeconds * 2.4).toFloat() + 1f) * 0.5f
        val rotFast = (timeSeconds * 42f) % 360f
        val rotSlow = (-timeSeconds * 26f) % 360f

        val w = size.width
        val h = size.height
        val horizonY = h * 0.54f
        val coreCenter = Offset(w * 0.5f, horizonY)

        // 1. Breathing neon reactor core glow
        val coreRadius = w * (0.34f + pulseWave * 0.12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = if (highContrast) 0.26f else 0.36f),
                    tertiary.copy(alpha = 0.15f),
                    Color.Transparent,
                ),
                center = coreCenter,
                radius = coreRadius,
            ),
            radius = coreRadius,
            center = coreCenter,
        )

        // 2. Counter-rotating cyber HUD segmented reactor arcs
        val innerArcRadius = minOf(w, h) * 0.15f
        val outerArcRadius = minOf(w, h) * 0.23f
        for (seg in 0 until 3) {
            drawArc(
                color = primary.copy(alpha = 0.48f),
                startAngle = rotFast + seg * 120f,
                sweepAngle = 68f,
                useCenter = false,
                topLeft = Offset(coreCenter.x - innerArcRadius, coreCenter.y - innerArcRadius),
                size = Size(innerArcRadius * 2f, innerArcRadius * 2f),
                style = Stroke(width = 2.6f, cap = StrokeCap.Round),
            )
            drawArc(
                color = secondary.copy(alpha = 0.36f),
                startAngle = rotSlow + seg * 120f + 30f,
                sweepAngle = 54f,
                useCenter = false,
                topLeft = Offset(coreCenter.x - outerArcRadius, coreCenter.y - outerArcRadius),
                size = Size(outerArcRadius * 2f, outerArcRadius * 2f),
                style = Stroke(width = 1.8f, cap = StrokeCap.Round),
            )
        }

        // 3. Expanding concentric shockwave pulse rings
        for (ringIdx in 0..2) {
            val ringProgress = ((timeSeconds * 0.32f) + ringIdx / 3f) % 1f
            val ringRadius = (w * 0.05f) + (w * 0.44f * ringProgress)
            val ringAlpha = (1f - ringProgress) * (if (highContrast) 0.34f else 0.44f)
            drawCircle(
                color = primary.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = coreCenter,
                style = Stroke(width = 1.6f + (1f - ringProgress) * 2.0f),
            )
        }

        // 4. Continuously scrolling 3D perspective laser grid
        val horizontalSteps = 10
        for (i in 0 until horizontalSteps) {
            val t = ((i + gridPhase) / horizontalSteps.toFloat()).coerceIn(0f, 1f)
            val y = horizonY + (h - horizonY) * (t * t)
            val lineAlpha = (0.14f + 0.38f * t) * (if (highContrast) 0.9f else 1f)
            drawLine(
                color = primary.copy(alpha = lineAlpha),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.3f + (2.2f * t),
            )
        }

        val rayCount = 10
        for (i in -rayCount..rayCount) {
            val bottomX = w * 0.5f + i * (w * 0.125f)
            val endPt = Offset(bottomX, h)
            drawLine(
                color = primary.copy(alpha = 0.24f),
                start = coreCenter,
                end = endPt,
                strokeWidth = 1.3f,
            )

            // Shooting neon laser packets traveling along select perspective rays
            if (i % 2 == 0) {
                val packetT = ((timeSeconds * 0.65f) + (i + rayCount) * 0.17f) % 1f
                val tSq = packetT * packetT
                val pX = coreCenter.x + (endPt.x - coreCenter.x) * tSq
                val pY = coreCenter.y + (endPt.y - coreCenter.y) * tSq
                drawCircle(
                    color = secondary.copy(alpha = (0.3f + 0.7f * tSq).coerceIn(0f, 1f)),
                    radius = 2.2f + 2.8f * tSq,
                    center = Offset(pX, pY),
                )
            }
        }

        // 5. Glowing horizon laser beam
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    secondary.copy(alpha = 0.75f),
                    primary,
                    secondary.copy(alpha = 0.75f),
                    Color.Transparent,
                ),
            ),
            start = Offset(w * 0.03f, horizonY),
            end = Offset(w * 0.97f, horizonY),
            strokeWidth = 2.8f,
        )

        // 6. Sweeping holographic scanline
        val scanPhase = (timeSeconds * 0.28f) % 1f
        val scanY = h * scanPhase
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    primary.copy(alpha = 0.14f),
                    secondary.copy(alpha = 0.24f),
                    Color.Transparent,
                ),
                startY = (scanY - 36f).coerceAtLeast(0f),
                endY = (scanY + 36f).coerceAtMost(h),
            ),
        )

        // 7. Rising neon cyber embers / data nodes
        val nodeCount = 18
        for (idx in 0 until nodeCount) {
            val speed = 0.14f + (idx % 4) * 0.045f
            val seed = (idx * 0.618034f) % 1f
            val nodeProgress = ((timeSeconds * speed) + seed) % 1f
            val xBase = w * ((idx + 0.5f) / nodeCount.toFloat())
            val xWobble = cos((timeSeconds * 1.6f + idx).toDouble()).toFloat() * 34f
            val x = (xBase + xWobble).coerceIn(0f, w)
            val y = h * (1f - nodeProgress)
            val nodeAlpha = sin(nodeProgress * PI).toFloat().coerceIn(0f, 1f) * 0.72f
            val radius = 2.4f + (idx % 3) * 1.6f
            drawCircle(
                color = if (idx % 2 == 0) primary.copy(alpha = nodeAlpha) else secondary.copy(alpha = nodeAlpha),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}
