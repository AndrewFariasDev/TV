package dev.neonlauncher.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
 * Compact glassmorphic TV quick-control tile (Settings, Wi-Fi, Bluetooth, Display, Inputs, Screensaver).
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
    val shape = RoundedCornerShape(14.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.06f),
        border = NeonFocusEffects.cardBorder(palette, shape),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 10.dp),
        modifier = modifier
            .widthIn(min = 152.dp, max = 220.dp)
            .heightIn(min = 54.dp)
            .semantics { contentDescription = cd },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                height = 24.dp,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isFocused) {
                            palette.accentPrimary.copy(alpha = 0.24f)
                        } else {
                            palette.accentPrimary.copy(alpha = 0.12f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconForQuickAction(action.type)),
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!action.subtitle.isNullOrBlank()) {
                    Text(
                        text = action.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
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
