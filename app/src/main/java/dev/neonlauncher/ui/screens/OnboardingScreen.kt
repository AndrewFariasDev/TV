package dev.neonlauncher.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import dev.neonlauncher.core.DeviceCapabilities
import dev.neonlauncher.ui.components.LeadingFocusIndicator
import dev.neonlauncher.ui.components.NeonFocusEffects
import dev.neonlauncher.ui.theme.LocalNeonPalette
import dev.neonlauncher.viewmodel.LauncherActions
import dev.neonlauncher.viewmodel.LauncherUiState

/**
 * First-run TV onboarding overlay presenting 3 D-pad quick-start steps, live device
 * compatibility inspection, Home-role setup, and Skip/Continue action.
 */
@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    uiState: LauncherUiState,
    actions: LauncherActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val primaryFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { primaryFocus.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.backgroundStart, palette.backgroundEnd),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.accentPrimary.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary,
                    maxLines = 1,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = palette.textPrimary,
                )
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OnboardingStepCard(
                    title = stringResource(R.string.onboarding_step_1_title),
                    description = stringResource(R.string.onboarding_step_1_desc),
                    accentColor = palette.accentPrimary,
                    modifier = Modifier.weight(1f),
                )
                OnboardingStepCard(
                    title = stringResource(R.string.onboarding_step_2_title),
                    description = stringResource(R.string.onboarding_step_2_desc),
                    accentColor = palette.accentSecondary,
                    modifier = Modifier.weight(1f),
                )
                OnboardingStepCard(
                    title = stringResource(R.string.onboarding_step_3_title),
                    description = stringResource(R.string.onboarding_step_3_desc),
                    accentColor = palette.accentTertiary,
                    modifier = Modifier.weight(1f),
                )
            }

            CompatibilitySummaryPanel(capabilities = uiState.capabilities)

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isHome = uiState.capabilities.isDefaultHome
                OnboardingActionButton(
                    iconRes = if (isHome) R.drawable.ic_neon_check else R.drawable.ic_neon_home,
                    label = stringResource(
                        if (isHome) R.string.onboarding_btn_home_active else R.string.onboarding_btn_set_home,
                    ),
                    isPrimary = !isHome,
                    onClick = actions::onRequestSetDefaultHome,
                    modifier = if (!isHome) Modifier.focusRequester(primaryFocus) else Modifier,
                )

                OnboardingActionButton(
                    iconRes = R.drawable.ic_neon_check,
                    label = stringResource(R.string.onboarding_btn_continue),
                    isPrimary = isHome,
                    onClick = actions::onCompleteOnboarding,
                    modifier = if (isHome) Modifier.focusRequester(primaryFocus) else Modifier,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OnboardingStepCard(
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = palette.surface,
            contentColor = palette.textPrimary,
        ),
        border = Border(
            border = BorderStroke(
                width = if (palette.highContrast) 1.5.dp else 1.dp,
                color = accentColor.copy(alpha = 0.45f),
            ),
            shape = shape,
        ),
        modifier = modifier.heightIn(min = 122.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CompatibilitySummaryPanel(
    capabilities: DeviceCapabilities,
) {
    val palette = LocalNeonPalette.current
    val shape = RoundedCornerShape(16.dp)

    Surface(
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = palette.surface,
            contentColor = palette.textPrimary,
        ),
        border = Border(
            border = BorderStroke(
                width = if (palette.highContrast) 1.5.dp else 1.dp,
                color = palette.surfaceBorder,
            ),
            shape = shape,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_neon_shield),
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.onboarding_compat_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompatChip(
                    label = stringResource(
                        R.string.onboarding_compat_android,
                        capabilities.androidRelease,
                        capabilities.sdkInt,
                    ),
                    accentColor = palette.accentPrimary,
                )
                CompatChip(
                    label = stringResource(
                        if (capabilities.hasLeanbackFeature) {
                            R.string.onboarding_compat_leanback_yes
                        } else {
                            R.string.onboarding_compat_leanback_no
                        },
                    ),
                    accentColor = palette.accentSecondary,
                )
                CompatChip(
                    label = stringResource(
                        if (capabilities.isLowPerformanceByDefault) {
                            R.string.onboarding_compat_tier_low
                        } else {
                            R.string.onboarding_compat_tier_full
                        },
                    ),
                    accentColor = palette.accentTertiary,
                )
                CompatChip(
                    label = stringResource(
                        if (capabilities.supportsBlur) {
                            R.string.onboarding_compat_blur_yes
                        } else {
                            R.string.onboarding_compat_blur_no
                        },
                    ),
                    accentColor = palette.accentPrimary,
                )
                CompatChip(
                    label = stringResource(
                        if (capabilities.supportsRoleManager) {
                            R.string.onboarding_compat_role_yes
                        } else {
                            R.string.onboarding_compat_role_no
                        },
                    ),
                    accentColor = palette.accentSecondary,
                )
            }
        }
    }
}

@Composable
private fun CompatChip(
    label: String,
    accentColor: Color,
) {
    val palette = LocalNeonPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accentColor.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OnboardingActionButton(
    @DrawableRes iconRes: Int,
    label: String,
    isPrimary: Boolean,
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
        colors = NeonFocusEffects.cardColors(
            palette = palette,
            containerColor = if (isPrimary) palette.accentPrimary.copy(alpha = 0.22f) else palette.surface,
            focusedContainerColor = if (isPrimary) palette.accentPrimary.copy(alpha = 0.34f) else palette.surfaceFocused,
        ),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.05f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            isSelected = isPrimary,
            accentStart = palette.accentPrimary,
            accentEnd = palette.accentSecondary,
        ),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 14.dp),
        modifier = modifier
            .widthIn(min = 190.dp)
            .heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (isPrimary && !palette.highContrast) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    palette.accentPrimary.copy(alpha = 0.14f),
                                    palette.accentSecondary.copy(alpha = 0.10f),
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                height = 18.dp,
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isPrimary || isFocused) palette.accentPrimary else palette.textPrimary,
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
