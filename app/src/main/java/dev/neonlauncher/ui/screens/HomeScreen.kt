package dev.neonlauncher.ui.screens

import android.text.format.DateFormat
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import dev.neonlauncher.core.AppIconLoader
import dev.neonlauncher.data.NeonThemeConfig
import dev.neonlauncher.ui.components.AppCard
import dev.neonlauncher.ui.components.ChooseAppsHudCard
import dev.neonlauncher.ui.components.LeadingFocusIndicator
import dev.neonlauncher.ui.components.NeonFocusEffects
import dev.neonlauncher.ui.components.QuickActionCard
import dev.neonlauncher.ui.theme.LocalNeonPalette
import dev.neonlauncher.ui.theme.NeonTypography
import dev.neonlauncher.viewmodel.ActiveOverlay
import dev.neonlauncher.viewmodel.LauncherActions
import dev.neonlauncher.viewmodel.LauncherUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Primary TV Home screen featuring clock/date header, ONLY the user's selected Home apps
 * (plus a quick "+ Choose Apps" selector card), device Quick Controls row, and D-pad shortcut bar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: LauncherUiState,
    iconLoader: AppIconLoader,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val locale = LocalLocale.current.platformLocale

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val millisIntoMinute = nowMillis % 60_000L
            delay((60_000L - millisIntoMinute + 50L).coerceAtLeast(500L))
        }
    }

    val firstCardFocus = remember { FocusRequester() }
    val isHomeActive = uiState.activeOverlay == ActiveOverlay.NONE &&
        uiState.contextMenuApp == null &&
        !uiState.isLoading

    LaunchedEffect(isHomeActive, uiState.favoriteApps.size) {
        if (isHomeActive) {
            runCatching { firstCardFocus.requestFocus() }
        }
    }

    val formattedTime = remember(nowMillis, uiState.use24HourClock, locale) {
        val pattern = if (uiState.use24HourClock) "HH:mm" else "h:mm a"
        SimpleDateFormat(pattern, locale).format(Date(nowMillis))
    }
    val formattedDate = remember(nowMillis, locale) {
        val bestPattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMd")
        SimpleDateFormat(bestPattern, locale).format(Date(nowMillis))
    }
    val themeDisplayName = localizedThemeName(uiState.activeTheme, locale)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 27.dp),
    ) {
        HomeHeaderBar(
            timeText = formattedTime,
            dateText = "$formattedDate  •  $themeDisplayName",
            isDefaultHome = uiState.capabilities.isDefaultHome,
            onChooseAppsClick = actions::onOpenAppPicker,
            onSetupClick = actions::onOpenOnboarding,
            onSettingsClick = actions::onOpenSettings,
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "user_apps_section") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionTitleRow(
                            title = stringResource(R.string.home_section_favorites),
                            badgeText = stringResource(R.string.home_favorites_count, uiState.favoriteApps.size),
                            accentColor = palette.accentPrimary,
                        )
                    }

                    if (uiState.favoriteApps.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            itemsIndexed(
                                items = uiState.favoriteApps,
                                key = { _, app -> "fav_${app.packageName}_${app.activityName}" },
                            ) { index, app ->
                                AppCard(
                                    app = app,
                                    iconLoader = iconLoader,
                                    isFavorite = true,
                                    featured = true,
                                    onClick = { actions.onLaunchApp(app) },
                                    onOpenContextMenu = { actions.onOpenContextMenu(app) },
                                    modifier = if (index == 0) Modifier.focusRequester(firstCardFocus) else Modifier,
                                )
                            }

                            item(key = "choose_more_apps_tile") {
                                ChooseAppsHudCard(
                                    onClick = actions::onOpenAppPicker,
                                    featured = true,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChooseAppsHudCard(
                                onClick = actions::onOpenAppPicker,
                                featured = true,
                                modifier = Modifier.focusRequester(firstCardFocus),
                            )
                            EmptyAppsBanner(
                                onChooseApps = actions::onOpenAppPicker,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (uiState.quickActions.isNotEmpty()) {
                item(key = "quick_actions_section") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionTitleRow(
                            title = stringResource(R.string.home_section_quick_actions),
                            badgeText = null,
                            accentColor = palette.textSecondary,
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(
                                items = uiState.quickActions,
                                key = { _, action -> "qa_${action.type.name}" },
                            ) { _, action ->
                                QuickActionCard(
                                    action = action,
                                    onClick = { actions.onQuickAction(action) },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        DpadHintFooter(
            hintText = stringResource(R.string.home_hint_bar),
        )
    }
}

@Composable
private fun HomeHeaderBar(
    timeText: String,
    dateText: String,
    isDefaultHome: Boolean,
    onChooseAppsClick: () -> Unit,
    onSetupClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val palette = LocalNeonPalette.current

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
                text = timeText,
                style = NeonTypography.clockStyle,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderPillButton(
                label = stringResource(R.string.home_button_choose_apps),
                iconRes = R.drawable.ic_neon_star,
                accentDotColor = palette.accentPrimary,
                onClick = onChooseAppsClick,
            )

            val setupDescription = stringResource(R.string.cd_setup_button)
            HeaderPillButton(
                label = stringResource(
                    if (isDefaultHome) R.string.home_button_default_home_active else R.string.home_button_setup,
                ),
                iconRes = if (isDefaultHome) R.drawable.ic_neon_check else R.drawable.ic_neon_home,
                accentDotColor = if (isDefaultHome) palette.accentPrimary else palette.accentSecondary,
                onClick = onSetupClick,
                modifier = Modifier.semantics { contentDescription = setupDescription },
            )

            val settingsDescription = stringResource(R.string.cd_settings_button)
            HeaderPillButton(
                label = stringResource(R.string.home_button_settings),
                iconRes = R.drawable.ic_neon_settings,
                accentDotColor = null,
                onClick = onSettingsClick,
                modifier = Modifier.semantics { contentDescription = settingsDescription },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderPillButton(
    label: String,
    @DrawableRes iconRes: Int,
    accentDotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(50)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.06f),
        border = NeonFocusEffects.cardBorder(palette, shape),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 10.dp),
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                height = 16.dp,
            )
            if (accentDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentDotColor),
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isFocused) palette.accentPrimary else palette.textPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionTitleRow(
    title: String,
    badgeText: String?,
    accentColor: Color,
) {
    val palette = LocalNeonPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (badgeText != null) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EmptyAppsBanner(
    onChooseApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)
    Surface(
        onClick = onChooseApps,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.02f),
        border = NeonFocusEffects.cardBorder(palette, shape),
        glow = NeonFocusEffects.cardGlow(palette),
        modifier = modifier.heightIn(min = 128.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_apps_title),
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
            )
            Text(
                text = stringResource(R.string.home_empty_apps_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DpadHintFooter(
    hintText: String,
) {
    val palette = LocalNeonPalette.current
    val shape = RoundedCornerShape(50)
    Surface(
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = palette.surface,
            contentColor = palette.textSecondary,
        ),
        border = Border(
            border = androidx.compose.foundation.BorderStroke(
                width = if (palette.highContrast) 1.5.dp else 1.dp,
                color = palette.surfaceBorder,
            ),
            shape = shape,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = hintText,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

fun localizedThemeName(theme: NeonThemeConfig, locale: Locale): String =
    if (locale.language.equals("es", ignoreCase = true) && !theme.nameEs.isNullOrBlank()) {
        theme.nameEs!!
    } else {
        theme.name
    }
