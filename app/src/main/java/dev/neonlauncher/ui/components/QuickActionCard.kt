package dev.neonlauncher.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.neonlauncher.R
import dev.neonlauncher.data.QuickAction
import dev.neonlauncher.data.QuickActionType
import dev.neonlauncher.ui.theme.LocalNeonPalette

/**
 * Minimalist futuristic cyber-blade Quick Control chip (Wi-Fi / TV Inputs).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val label = stringResource(action.labelRes)
    val cd = stringResource(R.string.cd_quick_action, label)
    val shape = CutCornerShape(topStart = 9.dp, bottomEnd = 9.dp, topEnd = 2.dp, bottomStart = 2.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(
            palette = palette,
            containerColor = palette.backgroundStart.copy(alpha = if (palette.highContrast) 0.90f else 0.72f),
            focusedContainerColor = palette.accentPrimary.copy(alpha = 0.18f),
        ),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.8.dp,
                    color = palette.accentPrimary.copy(alpha = 0.42f),
                ),
                shape = shape,
            ),
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.6.dp,
                    color = palette.accentPrimary,
                ),
                shape = shape,
            ),
            pressedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.6.dp,
                    color = palette.accentSecondary,
                ),
                shape = shape,
            ),
        ),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 10.dp),
        modifier = modifier
            .heightIn(min = 38.dp)
            .semantics { contentDescription = cd },
    ) {
        Row(
            modifier = Modifier
                .drawCyberBladeTicks(
                    isFocused = isFocused,
                    primary = palette.accentPrimary,
                    secondary = palette.accentSecondary,
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                height = 14.dp,
            )
            Icon(
                painter = painterResource(iconForQuickAction(action.type)),
                contentDescription = null,
                tint = if (isFocused) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.88f),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isFocused) palette.textPrimary else palette.textPrimary.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.drawCyberBladeTicks(
    isFocused: Boolean,
    primary: Color,
    secondary: Color,
): Modifier = this.drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    val arm = if (isFocused) 7.dp.toPx() else 5.dp.toPx()
    val inset = 2.dp.toPx()
    val stroke = if (isFocused) 1.8.dp.toPx() else 1.1.dp.toPx()
    val topCol = if (isFocused) primary else primary.copy(alpha = 0.6f)
    val botCol = if (isFocused) secondary else secondary.copy(alpha = 0.5f)

    // Top-left micro corner tick
    drawLine(topCol, Offset(inset, inset), Offset(inset + arm, inset), stroke)
    drawLine(topCol, Offset(inset, inset), Offset(inset, inset + arm), stroke)

    // Bottom-right micro corner tick
    drawLine(botCol, Offset(w - inset - arm, h - inset), Offset(w - inset, h - inset), stroke)
    drawLine(botCol, Offset(w - inset, h - inset - arm), Offset(w - inset, h - inset), stroke)
}

@DrawableRes
private fun iconForQuickAction(type: QuickActionType): Int = when (type) {
    QuickActionType.SETTINGS -> R.drawable.ic_neon_settings
    QuickActionType.WIFI -> R.drawable.ic_neon_wifi
    QuickActionType.BLUETOOTH -> R.drawable.ic_neon_bluetooth
    QuickActionType.DISPLAY -> R.drawable.ic_neon_display
    QuickActionType.TV_INPUTS -> R.drawable.ic_neon_tv_inputs
    QuickActionType.SCREENSAVER -> R.drawable.ic_neon_screensaver
}
