package dev.neonlauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import dev.neonlauncher.data.AppInfo
import dev.neonlauncher.ui.theme.LocalNeonPalette

/**
 * D-pad modal menu for managing an app from the Home grid (Pin, Hide, App Info, Uninstall, Close).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppContextMenuDialog(
    app: AppInfo,
    isFavorite: Boolean,
    onToggleFavorite: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onUninstallApp: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val firstOptionFocus = remember { FocusRequester() }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(app.packageName, app.activityName) {
        runCatching { firstOptionFocus.requestFocus() }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Scrim background (optionally blurred on API 31+ when Performance Mode is off)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .neonAmbientBlur(radius = 12.dp, performanceMode = palette.performanceMode)
                .background(Color.Black.copy(alpha = if (palette.highContrast) 0.82f else 0.70f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        val modalShape = RoundedCornerShape(22.dp)
        Surface(
            shape = modalShape,
            colors = SurfaceDefaults.colors(
                containerColor = palette.surfaceModal,
                contentColor = palette.textPrimary,
            ),
            border = Border(
                border = BorderStroke(
                    width = if (palette.highContrast) 2.5.dp else 1.8.dp,
                    brush = Brush.linearGradient(
                        listOf(palette.accentPrimary, palette.accentSecondary),
                    ),
                ),
                shape = modalShape,
            ),
            glow = if (palette.performanceMode) {
                Glow.None
            } else {
                Glow(
                    elevationColor = palette.accentPrimary.copy(alpha = 0.28f),
                    elevation = 18.dp,
                )
            },
            modifier = Modifier
                .padding(horizontal = 48.dp, vertical = 27.dp)
                .widthIn(min = 360.dp, max = 460.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.context_menu_title).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.accentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(
                            if (app.isLeanback) {
                                stringResource(R.string.context_menu_subtitle_leanback)
                            } else {
                                stringResource(R.string.context_menu_subtitle_standard)
                            },
                        )
                        append("  •  ")
                        append(app.packageName)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                ContextMenuOptionButton(
                    iconRes = if (isFavorite) R.drawable.ic_neon_star_outline else R.drawable.ic_neon_star,
                    label = stringResource(
                        if (isFavorite) {
                            R.string.context_menu_unpin_favorite
                        } else {
                            R.string.context_menu_pin_favorite
                        },
                    ),
                    onClick = {
                        onToggleFavorite(app)
                        onDismiss()
                    },
                    modifier = Modifier.focusRequester(firstOptionFocus),
                )

                ContextMenuOptionButton(
                    iconRes = R.drawable.ic_neon_hide,
                    label = stringResource(R.string.context_menu_hide_app),
                    onClick = {
                        onHideApp(app)
                        onDismiss()
                    },
                )

                ContextMenuOptionButton(
                    iconRes = R.drawable.ic_neon_info,
                    label = stringResource(R.string.context_menu_app_info),
                    onClick = {
                        onOpenAppInfo(app)
                        onDismiss()
                    },
                )

                if (!app.isSystemApp) {
                    ContextMenuOptionButton(
                        iconRes = R.drawable.ic_neon_uninstall,
                        label = stringResource(R.string.context_menu_uninstall),
                        isDanger = true,
                        onClick = {
                            onUninstallApp(app)
                            onDismiss()
                        },
                    )
                }

                ContextMenuOptionButton(
                    iconRes = R.drawable.ic_neon_close,
                    label = stringResource(R.string.context_menu_close),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ContextMenuOptionButton(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(13.dp)
    val accent = if (isDanger) palette.danger else palette.accentPrimary
    val contentColor = if (isDanger) palette.danger else palette.textPrimary

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(
            palette = palette,
            contentColor = contentColor,
        ),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.03f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            accentStart = accent,
            accentEnd = if (isDanger) palette.danger else palette.accentSecondary,
        ),
        glow = NeonFocusEffects.cardGlow(palette, accentColor = accent, elevation = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                accentColor = accent,
                height = 20.dp,
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isFocused || isDanger) accent else palette.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
