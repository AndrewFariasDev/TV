package dev.neonlauncher.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.neonlauncher.R
import dev.neonlauncher.ui.components.LeadingFocusIndicator
import dev.neonlauncher.ui.components.NeonFocusEffects
import dev.neonlauncher.ui.theme.LocalNeonPalette
import dev.neonlauncher.viewmodel.LauncherActions
import dev.neonlauncher.viewmodel.LauncherUiState

/**
 * Recovery screen displayed when the Safe Mode watchdog disables the Home alias
 * after 3 rapid crashes within 2 minutes.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SafeModeRecoveryScreen(
    uiState: LauncherUiState,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val initialFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { initialFocus.requestFocus() }
    }

    val panelShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.84f))
            .padding(horizontal = 48.dp, vertical = 27.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = panelShape,
            colors = SurfaceDefaults.colors(
                containerColor = palette.surfaceModal,
                contentColor = palette.textPrimary,
            ),
            border = Border(
                border = BorderStroke(
                    width = if (palette.highContrast) 2.5.dp else 1.8.dp,
                    color = palette.danger,
                ),
                shape = panelShape,
            ),
            glow = if (palette.performanceMode) {
                Glow.None
            } else {
                Glow(
                    elevationColor = palette.danger.copy(alpha = 0.25f),
                    elevation = 18.dp,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.danger.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_neon_warning),
                                contentDescription = null,
                                tint = palette.danger,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.safe_mode_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.danger,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.safe_mode_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary,
                )

                Text(
                    text = stringResource(R.string.safe_mode_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.safe_mode_crash_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.accentPrimary,
                    )
                    Text(
                        text = uiState.safeModeCrashSummary?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.safe_mode_no_details),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.textSecondary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SafeModeActionButton(
                        iconRes = R.drawable.ic_neon_home,
                        label = stringResource(R.string.safe_mode_btn_reenable),
                        accentColor = palette.accentPrimary,
                        onClick = actions::onSafeModeReenableHome,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(initialFocus),
                    )
                    SafeModeActionButton(
                        iconRes = R.drawable.ic_neon_check,
                        label = stringResource(R.string.safe_mode_btn_continue),
                        accentColor = palette.accentSecondary,
                        onClick = actions::onSafeModeContinueManual,
                        modifier = Modifier.weight(1f),
                    )
                    SafeModeActionButton(
                        iconRes = R.drawable.ic_neon_warning,
                        label = stringResource(R.string.safe_mode_btn_reset),
                        accentColor = palette.danger,
                        onClick = actions::onSafeModeResetAll,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SafeModeActionButton(
    @DrawableRes iconRes: Int,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.04f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            accentStart = accentColor,
            accentEnd = accentColor,
        ),
        glow = NeonFocusEffects.cardGlow(palette, accentColor = accentColor, elevation = 10.dp),
        modifier = modifier.heightIn(min = 50.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                accentColor = accentColor,
                height = 18.dp,
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
