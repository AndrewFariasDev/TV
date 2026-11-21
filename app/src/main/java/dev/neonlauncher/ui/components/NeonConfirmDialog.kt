package dev.neonlauncher.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.neonlauncher.ui.theme.LocalNeonPalette

/**
 * D-pad confirmation dialog for destructive or role-changing actions (Reset / Return to Stock).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NeonConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    isDanger: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val cancelFocus = remember { FocusRequester() }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        runCatching { cancelFocus.requestFocus() }
    }

    val accent = if (isDanger) palette.danger else palette.accentPrimary
    val modalShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .neonAmbientBlur(radius = 12.dp, performanceMode = palette.performanceMode)
                .background(Color.Black.copy(alpha = if (palette.highContrast) 0.85f else 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Surface(
            shape = modalShape,
            colors = SurfaceDefaults.colors(
                containerColor = palette.surfaceModal,
                contentColor = palette.textPrimary,
            ),
            border = Border(
                border = BorderStroke(
                    width = if (palette.highContrast) 2.5.dp else 1.8.dp,
                    color = accent,
                ),
                shape = modalShape,
            ),
            glow = if (palette.performanceMode) {
                Glow.None
            } else {
                Glow(
                    elevationColor = accent.copy(alpha = 0.28f),
                    elevation = 16.dp,
                )
            },
            modifier = Modifier
                .padding(horizontal = 48.dp, vertical = 27.dp)
                .widthIn(min = 380.dp, max = 520.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
                ) {
                    ConfirmDialogButton(
                        label = cancelLabel,
                        accentColor = palette.accentPrimary,
                        isPrimary = false,
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(cancelFocus),
                    )
                    ConfirmDialogButton(
                        label = confirmLabel,
                        accentColor = accent,
                        isPrimary = true,
                        onClick = {
                            onDismiss()
                            onConfirm()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ConfirmDialogButton(
    label: String,
    accentColor: Color,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(13.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(
            palette = palette,
            containerColor = if (isPrimary) accentColor.copy(alpha = 0.18f) else palette.surface,
            focusedContainerColor = if (isPrimary) accentColor.copy(alpha = 0.30f) else palette.surfaceFocused,
            contentColor = if (isPrimary) accentColor else palette.textPrimary,
        ),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.05f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            accentStart = accentColor,
            accentEnd = accentColor,
        ),
        glow = NeonFocusEffects.cardGlow(palette, accentColor = accentColor, elevation = 10.dp),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeadingFocusIndicator(
                isFocused = isFocused,
                palette = palette,
                accentColor = accentColor,
                height = 16.dp,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPrimary) accentColor else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
