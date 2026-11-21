package dev.neonlauncher.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.neonlauncher.R
import dev.neonlauncher.core.AppIconLoader
import dev.neonlauncher.ui.components.AppCard
import dev.neonlauncher.ui.components.LeadingFocusIndicator
import dev.neonlauncher.ui.components.NeonFocusEffects
import dev.neonlauncher.ui.theme.LocalNeonPalette
import dev.neonlauncher.viewmodel.LauncherActions
import dev.neonlauncher.viewmodel.LauncherUiState

/**
 * Full-screen opaque D-pad selector where the user chooses which installed applications
 * are displayed on the main Home menu.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppPickerScreen(
    uiState: LauncherUiState,
    iconLoader: AppIconLoader,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val initialFocus = remember { FocusRequester() }
    val rows = remember(uiState.allApps) { uiState.allApps.chunked(4) }

    LaunchedEffect(rows.isNotEmpty()) {
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
            AppPickerHeader(
                selectedCount = uiState.favoriteApps.size,
                totalCount = uiState.allApps.size,
                onDone = actions::onCloseOverlay,
                doneFocusRequester = if (rows.isEmpty()) initialFocus else null,
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                itemsIndexed(
                    items = rows,
                    key = { rowIndex, rowApps ->
                        "picker_row_${rowIndex}_${rowApps.firstOrNull()?.packageName.orEmpty()}"
                    },
                ) { rowIndex, rowApps ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        rowApps.forEachIndexed { colIndex, app ->
                            val isFirst = rowIndex == 0 && colIndex == 0
                            AppCard(
                                app = app,
                                iconLoader = iconLoader,
                                isFavorite = uiState.favoritePackages.contains(app.packageName),
                                featured = false,
                                pickerMode = true,
                                onClick = { actions.onToggleFavorite(app.packageName) },
                                onOpenContextMenu = { actions.onOpenContextMenu(app) },
                                modifier = if (isFirst) Modifier.focusRequester(initialFocus) else Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppPickerHeader(
    selectedCount: Int,
    totalCount: Int,
    onDone: () -> Unit,
    doneFocusRequester: FocusRequester? = null,
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
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_picker_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stringResource(R.string.home_favorites_count, selectedCount)}  •  ${stringResource(R.string.home_apps_count, totalCount)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentPrimary,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(R.string.app_picker_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val doneModifier = if (doneFocusRequester != null) {
            Modifier.focusRequester(doneFocusRequester)
        } else {
            Modifier
        }

        Surface(
            onClick = onDone,
            interactionSource = interactionSource,
            shape = NeonFocusEffects.cardShape(shape),
            colors = NeonFocusEffects.cardColors(palette),
            scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.05f),
            border = NeonFocusEffects.cardBorder(palette, shape, isSelected = true),
            glow = NeonFocusEffects.cardGlow(palette, elevation = 12.dp),
            modifier = doneModifier.heightIn(min = 42.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LeadingFocusIndicator(
                    isFocused = isFocused,
                    palette = palette,
                    height = 16.dp,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_neon_check),
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.app_picker_done),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}
