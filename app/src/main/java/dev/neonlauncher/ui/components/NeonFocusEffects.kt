package dev.neonlauncher.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceGlow
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import dev.neonlauncher.ui.theme.NeonPalette

/**
 * Shared D-pad focus and glassmorphism tokens for `androidx.tv.material3.Surface`.
 */
object NeonFocusEffects {
    const val FocusAnimationMs: Int = 200

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun cardColors(
        palette: NeonPalette,
        containerColor: Color = palette.surface,
        focusedContainerColor: Color = palette.surfaceFocused,
        contentColor: Color = palette.textPrimary,
    ): ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = contentColor,
        pressedContainerColor = focusedContainerColor,
        pressedContentColor = contentColor,
        disabledContainerColor = containerColor.copy(alpha = 0.4f),
        disabledContentColor = palette.textSecondary.copy(alpha = 0.5f),
    )

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun cardShape(shape: Shape): ClickableSurfaceShape = ClickableSurfaceDefaults.shape(
        shape = shape,
        focusedShape = shape,
        pressedShape = shape,
        disabledShape = shape,
    )

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun cardScale(
        performanceMode: Boolean,
        focusedScale: Float = 1.08f,
    ): ClickableSurfaceScale = ClickableSurfaceDefaults.scale(
        scale = 1f,
        focusedScale = if (performanceMode) 1f else focusedScale,
        pressedScale = if (performanceMode) 1f else 0.98f,
        disabledScale = 1f,
    )

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun cardBorder(
        palette: NeonPalette,
        shape: Shape,
        isSelected: Boolean = false,
        accentStart: Color = palette.accentPrimary,
        accentEnd: Color = palette.accentSecondary,
    ): ClickableSurfaceBorder {
        val restingWidth = when {
            palette.highContrast && isSelected -> 2.5.dp
            palette.highContrast -> 1.6.dp
            isSelected -> 1.8.dp
            else -> 1.dp
        }
        val focusedWidth = if (palette.highContrast) 3.5.dp else 2.5.dp
        val restingColor = when {
            isSelected -> accentStart.copy(alpha = 0.9f)
            palette.highContrast -> palette.surfaceBorder
            else -> palette.surfaceBorder
        }
        val focusedBrush = if (palette.highContrast) {
            Brush.linearGradient(listOf(palette.focusedBorder, accentStart))
        } else {
            Brush.linearGradient(listOf(accentStart, accentEnd))
        }

        return ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(restingWidth, restingColor),
                shape = shape,
            ),
            focusedBorder = Border(
                border = BorderStroke(focusedWidth, focusedBrush),
                shape = shape,
            ),
            pressedBorder = Border(
                border = BorderStroke(focusedWidth, focusedBrush),
                shape = shape,
            ),
            disabledBorder = Border(
                border = BorderStroke(1.dp, palette.surfaceBorder.copy(alpha = 0.3f)),
                shape = shape,
            ),
        )
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun cardGlow(
        palette: NeonPalette,
        accentColor: Color = palette.accentPrimary,
        elevation: Dp = 14.dp,
    ): ClickableSurfaceGlow = if (palette.performanceMode) {
        ClickableSurfaceDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow.None,
            pressedGlow = Glow.None,
        )
    } else {
        ClickableSurfaceDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow(
                elevationColor = accentColor.copy(alpha = if (palette.highContrast) 0.55f else 0.42f),
                elevation = elevation,
            ),
            pressedGlow = Glow(
                elevationColor = accentColor.copy(alpha = 0.35f),
                elevation = elevation / 2,
            ),
        )
    }
}

/**
 * Applies background blur strictly on Android 12+ (API 31+) and never in Performance Mode.
 * Never apply this modifier to interactive foreground text.
 */
fun Modifier.neonAmbientBlur(
    radius: Dp,
    performanceMode: Boolean,
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !performanceMode) {
    this.blur(radius = radius)
} else {
    this
}

/**
 * Non-color focus cue (bright leading accent pill bar) so D-pad focus remains unmistakable
 * regardless of color perception or display brightness.
 */
@Composable
fun LeadingFocusIndicator(
    isFocused: Boolean,
    palette: NeonPalette,
    modifier: Modifier = Modifier,
    accentColor: Color = palette.accentPrimary,
    height: Dp = 28.dp,
) {
    val targetColor = when {
        isFocused && palette.highContrast -> Color.White
        isFocused -> accentColor
        else -> Color.Transparent
    }
    val barColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = if (palette.performanceMode) 0 else NeonFocusEffects.FocusAnimationMs),
        label = "focusBarColor",
    )
    Box(
        modifier = modifier
            .width(if (isFocused) 4.dp else 0.dp)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(barColor),
    )
}
