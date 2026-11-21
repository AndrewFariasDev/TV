package dev.neonlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import dev.neonlauncher.core.AppIconLoader
import dev.neonlauncher.ui.components.AppContextMenuDialog
import dev.neonlauncher.ui.components.NeonBackground
import dev.neonlauncher.ui.theme.LocalNeonPalette
import dev.neonlauncher.ui.theme.NeonTheme
import dev.neonlauncher.viewmodel.ActiveOverlay
import dev.neonlauncher.viewmodel.LauncherActions
import dev.neonlauncher.viewmodel.LauncherUiState
import kotlinx.coroutines.delay

/**
 * Root Compose for TV entrypoint connecting [NeonTheme], [NeonBackground], [HomeScreen],
 * active modal overlays ([SettingsScreen], [OnboardingScreen], [SafeModeRecoveryScreen]),
 * [AppContextMenuDialog], transient status toast pill, and TV Back navigation guard.
 */
@Composable
fun NeonLauncherRoot(
    uiState: LauncherUiState,
    iconLoader: AppIconLoader,
    actions: LauncherActions,
) {
    BackHandler(enabled = true) {
        when {
            uiState.contextMenuApp != null -> actions.onDismissContextMenu()
            uiState.activeOverlay == ActiveOverlay.SETTINGS -> actions.onCloseOverlay()
            uiState.activeOverlay == ActiveOverlay.ONBOARDING -> actions.onCompleteOnboarding()
            else -> Unit // Never exit Home when pressing Back on the launcher root or Safe Mode.
        }
    }

    NeonTheme(
        themeConfig = uiState.activeTheme,
        highContrast = uiState.highContrastMode,
        performanceMode = uiState.performanceMode,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NeonBackground(
                style = uiState.effectiveBackgroundStyle,
                performanceMode = uiState.performanceMode,
            )

            HomeScreen(
                uiState = uiState,
                iconLoader = iconLoader,
                actions = actions,
            )

            when (uiState.activeOverlay) {
                ActiveOverlay.NONE -> Unit
                ActiveOverlay.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        actions = actions,
                    )
                }

                ActiveOverlay.ONBOARDING -> {
                    OnboardingScreen(
                        uiState = uiState,
                        actions = actions,
                    )
                }

                ActiveOverlay.SAFE_MODE -> {
                    SafeModeRecoveryScreen(
                        uiState = uiState,
                        actions = actions,
                    )
                }
            }

            val menuApp = uiState.contextMenuApp
            if (menuApp != null && uiState.activeOverlay == ActiveOverlay.NONE) {
                AppContextMenuDialog(
                    app = menuApp,
                    isFavorite = uiState.favoritePackages.contains(menuApp.packageName),
                    onToggleFavorite = { app -> actions.onToggleFavorite(app.packageName) },
                    onHideApp = { app -> actions.onHideApp(app.packageName) },
                    onOpenAppInfo = { app -> actions.onOpenAppInfo(app.packageName) },
                    onUninstallApp = { app -> actions.onUninstallApp(app.packageName) },
                    onDismiss = actions::onDismissContextMenu,
                )
            }

            val statusRes = uiState.statusMessageRes
            LaunchedEffect(statusRes) {
                if (statusRes != null) {
                    delay(3_500L)
                    actions.onClearStatusMessage()
                }
            }

            AnimatedVisibility(
                visible = statusRes != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 48.dp, vertical = 72.dp),
            ) {
                if (statusRes != null) {
                    StatusToastPill(message = stringResource(statusRes))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusToastPill(
    message: String,
) {
    val palette = LocalNeonPalette.current
    val shape = RoundedCornerShape(50)
    val bannerCd = stringResource(R.string.cd_status_banner)

    Surface(
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = palette.surfaceModal,
            contentColor = palette.textPrimary,
        ),
        border = Border(
            border = BorderStroke(
                width = if (palette.highContrast) 2.dp else 1.5.dp,
                color = palette.accentPrimary,
            ),
            shape = shape,
        ),
        glow = if (palette.performanceMode) {
            Glow.None
        } else {
            Glow(
                elevationColor = palette.accentPrimary.copy(alpha = 0.32f),
                elevation = 14.dp,
            )
        },
        modifier = Modifier.semantics { contentDescription = bannerCd },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_neon_info),
                contentDescription = null,
                tint = palette.accentPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
