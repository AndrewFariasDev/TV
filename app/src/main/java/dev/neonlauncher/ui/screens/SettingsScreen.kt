package dev.neonlauncher.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.neonlauncher.R
import dev.neonlauncher.data.BackgroundStyle
import dev.neonlauncher.data.NeonThemeConfig
import dev.neonlauncher.data.QuickAction
import dev.neonlauncher.data.QuickActionType
import dev.neonlauncher.ui.components.LeadingFocusIndicator
import dev.neonlauncher.ui.components.NeonConfirmDialog
import dev.neonlauncher.ui.components.NeonFocusEffects
import dev.neonlauncher.ui.theme.LocalNeonPalette
import dev.neonlauncher.viewmodel.LauncherActions
import dev.neonlauncher.viewmodel.LauncherUiState
import java.util.Locale

private enum class PendingConfirm {
    NONE,
    RESET_SETTINGS,
    RETURN_TO_STOCK,
}

/**
 * Full-screen D-pad Settings overlay covering Themes, Background Style, Clock & Accessibility,
 * Hidden Apps restoration, Screensaver & Default Home setup, and Reset / Stock Recovery.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: LauncherUiState,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val locale = LocalLocale.current.platformLocale
    val initialFocus = remember { FocusRequester() }
    var pendingConfirm by remember { mutableStateOf(PendingConfirm.NONE) }

    LaunchedEffect(Unit) {
        runCatching { initialFocus.requestFocus() }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp),
        ) {
            SettingsHeader(
                onClose = actions::onCloseOverlay,
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "section_theme") {
                    SettingsSectionLabel(text = stringResource(R.string.settings_section_theme))
                }

                item(key = "themes_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.themes,
                            key = { _, theme -> "theme_${theme.id}" },
                        ) { index, theme ->
                            ThemeSwatchCard(
                                theme = theme,
                                displayName = localizedThemeName(theme, locale),
                                isSelected = theme.id == uiState.activeTheme.id,
                                onClick = { actions.onSelectTheme(theme.id) },
                                modifier = if (index == 0) Modifier.focusRequester(initialFocus) else Modifier,
                            )
                        }
                    }
                }

                item(key = "section_background") {
                    SettingsSectionLabel(text = stringResource(R.string.settings_section_background))
                }

                item(key = "background_styles") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = BackgroundStyle.entries,
                            key = { "bg_${it.name}" },
                        ) { style ->
                            BackgroundStyleChip(
                                style = style,
                                isSelected = uiState.effectiveBackgroundStyle == style,
                                onClick = { actions.onSelectBackgroundStyle(style) },
                            )
                        }
                    }
                }

                item(key = "section_display") {
                    SettingsSectionLabel(text = stringResource(R.string.settings_section_display))
                }

                item(key = "toggle_24h") {
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_clock_24h_title),
                        description = stringResource(R.string.settings_clock_24h_desc),
                        checked = uiState.use24HourClock,
                        onToggle = { actions.onToggle24HourClock(!uiState.use24HourClock) },
                    )
                }

                item(key = "toggle_perf") {
                    val desc = buildString {
                        append(stringResource(R.string.settings_perf_mode_desc))
                        if (uiState.performanceModeIsAuto) {
                            append(" • ")
                            append(stringResource(R.string.settings_perf_mode_auto_hint))
                        }
                    }
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_perf_mode_title),
                        description = desc,
                        checked = uiState.performanceMode,
                        onToggle = { actions.onTogglePerformanceMode(!uiState.performanceMode) },
                    )
                }

                item(key = "toggle_high_contrast") {
                    SettingsToggleRow(
                        title = stringResource(R.string.settings_high_contrast_title),
                        description = stringResource(R.string.settings_high_contrast_desc),
                        checked = uiState.highContrastMode,
                        onToggle = { actions.onToggleHighContrast(!uiState.highContrastMode) },
                    )
                }

                item(key = "section_hidden_apps") {
                    SettingsSectionLabel(text = stringResource(R.string.settings_section_hidden_apps))
                }

                if (uiState.hiddenApps.isEmpty()) {
                    item(key = "no_hidden_apps") {
                        SettingsInfoRow(text = stringResource(R.string.settings_no_hidden_apps))
                    }
                } else {
                    items(
                        items = uiState.hiddenApps,
                        key = { "hidden_${it.packageName}_${it.activityName}" },
                    ) { hiddenApp ->
                        SettingsActionRow(
                            iconRes = R.drawable.ic_neon_unhide,
                            title = stringResource(R.string.settings_unhide_action, hiddenApp.label),
                            description = "${hiddenApp.packageName}  •  ${stringResource(R.string.settings_unhide_hint)}",
                            onClick = { actions.onUnhideApp(hiddenApp.packageName) },
                        )
                    }
                }

                item(key = "section_system") {
                    SettingsSectionLabel(text = stringResource(R.string.settings_section_system))
                }

                item(key = "action_choose_apps") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_neon_star,
                        title = stringResource(R.string.app_picker_title),
                        description = stringResource(R.string.settings_choose_apps_desc),
                        onClick = actions::onOpenAppPicker,
                    )
                }

                item(key = "action_tv_system_settings") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_neon_settings,
                        title = stringResource(R.string.quick_action_settings),
                        description = stringResource(R.string.settings_tv_system_desc),
                        onClick = {
                            actions.onQuickAction(
                                QuickAction(
                                    type = QuickActionType.SETTINGS,
                                    labelRes = R.string.quick_action_settings,
                                ),
                            )
                        },
                    )
                }

                if (uiState.capabilities.supportsDreamService) {
                    item(key = "action_screensaver") {
                        SettingsActionRow(
                            iconRes = R.drawable.ic_neon_screensaver,
                            title = stringResource(R.string.settings_screensaver_title),
                            description = stringResource(R.string.settings_screensaver_desc),
                            onClick = {
                                val action = uiState.quickActions.firstOrNull {
                                    it.type == QuickActionType.SCREENSAVER
                                } ?: QuickAction(
                                    type = QuickActionType.SCREENSAVER,
                                    labelRes = R.string.quick_action_screensaver,
                                )
                                actions.onQuickAction(action)
                            },
                        )
                    }
                }

                item(key = "action_default_home") {
                    SettingsActionRow(
                        iconRes = if (uiState.capabilities.isDefaultHome) {
                            R.drawable.ic_neon_check
                        } else {
                            R.drawable.ic_neon_home
                        },
                        title = if (uiState.capabilities.isDefaultHome) {
                            stringResource(R.string.settings_set_default_home_active)
                        } else {
                            stringResource(R.string.settings_set_default_home_title)
                        },
                        description = stringResource(R.string.settings_set_default_home_desc),
                        onClick = actions::onRequestSetDefaultHome,
                    )
                }

                item(key = "action_onboarding") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_neon_info,
                        title = stringResource(R.string.settings_open_onboarding_title),
                        description = stringResource(R.string.settings_open_onboarding_desc),
                        onClick = actions::onOpenOnboarding,
                    )
                }

                item(key = "section_recovery") {
                    SettingsSectionLabel(text = stringResource(R.string.settings_section_recovery))
                }

                item(key = "action_reset") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_neon_warning,
                        title = stringResource(R.string.settings_reset_title),
                        description = stringResource(R.string.settings_reset_desc),
                        isDanger = false,
                        onClick = { pendingConfirm = PendingConfirm.RESET_SETTINGS },
                    )
                }

                item(key = "action_return_stock") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_neon_home,
                        title = stringResource(R.string.settings_return_stock_title),
                        description = stringResource(R.string.settings_return_stock_desc),
                        isDanger = true,
                        onClick = { pendingConfirm = PendingConfirm.RETURN_TO_STOCK },
                    )
                }
            }
        }

        when (pendingConfirm) {
            PendingConfirm.RESET_SETTINGS -> {
                NeonConfirmDialog(
                    title = stringResource(R.string.confirm_reset_title),
                    message = stringResource(R.string.confirm_reset_message),
                    confirmLabel = stringResource(R.string.confirm_reset_positive),
                    cancelLabel = stringResource(R.string.confirm_cancel),
                    isDanger = true,
                    onConfirm = actions::onResetSettings,
                    onDismiss = { pendingConfirm = PendingConfirm.NONE },
                )
            }

            PendingConfirm.RETURN_TO_STOCK -> {
                NeonConfirmDialog(
                    title = stringResource(R.string.confirm_stock_title),
                    message = stringResource(R.string.confirm_stock_message),
                    confirmLabel = stringResource(R.string.confirm_stock_positive),
                    cancelLabel = stringResource(R.string.confirm_cancel),
                    isDanger = true,
                    onConfirm = actions::onReturnToStockLauncher,
                    onDismiss = { pendingConfirm = PendingConfirm.NONE },
                )
            }

            PendingConfirm.NONE -> Unit
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsHeader(
    onClose: () -> Unit,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(50)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            onClick = onClose,
            interactionSource = interactionSource,
            shape = NeonFocusEffects.cardShape(shape),
            colors = NeonFocusEffects.cardColors(palette),
            scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.05f),
            border = NeonFocusEffects.cardBorder(palette, shape),
            glow = NeonFocusEffects.cardGlow(palette, elevation = 10.dp),
            modifier = Modifier.heightIn(min = 42.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LeadingFocusIndicator(
                    isFocused = isFocused,
                    palette = palette,
                    height = 16.dp,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_neon_close),
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.settings_close),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    val palette = LocalNeonPalette.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = palette.accentPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemeSwatchCard(
    theme: NeonThemeConfig,
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val swatchPrimary = Color(theme.colors.accentPrimary)
    val swatchSecondary = Color(theme.colors.accentSecondary)
    val cd = stringResource(R.string.cd_theme_swatch, displayName)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.05f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            isSelected = isSelected,
            accentStart = swatchPrimary,
            accentEnd = swatchSecondary,
        ),
        glow = NeonFocusEffects.cardGlow(palette, accentColor = swatchPrimary, elevation = 12.dp),
        modifier = modifier
            .width(224.dp)
            .heightIn(min = 68.dp)
            .semantics { contentDescription = cd },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                accentColor = swatchPrimary,
                height = 28.dp,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(swatchPrimary, swatchSecondary),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_neon_check),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = backgroundStyleLabel(theme.backgroundStyle),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) swatchPrimary else palette.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackgroundStyleChip(
    style: BackgroundStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
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
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.05f),
        border = NeonFocusEffects.cardBorder(palette, shape, isSelected = isSelected),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 10.dp),
        modifier = Modifier.heightIn(min = 44.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                height = 16.dp,
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_neon_check),
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = backgroundStyleLabel(style),
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)

    Surface(
        onClick = onToggle,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.02f),
        border = NeonFocusEffects.cardBorder(palette, shape),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                height = 28.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (checked) {
                            palette.accentPrimary.copy(alpha = 0.22f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (checked) R.string.settings_state_on else R.string.settings_state_off),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (checked) palette.accentPrimary else palette.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsActionRow(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    isDanger: Boolean = false,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val accent = if (isDanger) palette.danger else palette.accentPrimary

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.02f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            accentStart = accent,
            accentEnd = if (isDanger) palette.danger else palette.accentSecondary,
        ),
        glow = NeonFocusEffects.cardGlow(palette, accentColor = accent, elevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                accentColor = accent,
                height = 28.dp,
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isDanger || isFocused) accent else palette.textSecondary,
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDanger) palette.danger else palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsInfoRow(text: String) {
    val palette = LocalNeonPalette.current
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = palette.surface,
            contentColor = palette.textSecondary,
        ),
        border = Border(
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.surfaceBorder),
            shape = shape,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun backgroundStyleLabel(style: BackgroundStyle): String = when (style) {
    BackgroundStyle.AURORA -> stringResource(R.string.bg_style_aurora)
    BackgroundStyle.GRID -> stringResource(R.string.bg_style_grid)
    BackgroundStyle.CYBER_PULSE -> stringResource(R.string.bg_style_cyber_pulse)
    BackgroundStyle.SOLID -> stringResource(R.string.bg_style_solid)
}
