package dev.neonlauncher.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.neonlauncher.R
import dev.neonlauncher.core.AppIconLoader
import dev.neonlauncher.data.AppInfo
import dev.neonlauncher.ui.theme.LocalNeonPalette

/**
 * Futuristic Cyber-HUD TV app tile featuring chamfered tech framing, glowing corner reticles,
 * top laser telemetry strip, holographic icon pedestal (or full-bleed TV banner), and high-contrast
 * bottom HUD nameplate.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppCard(
    app: AppInfo,
    iconLoader: AppIconLoader,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onOpenContextMenu: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    pickerMode: Boolean = false,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val iconState by produceState<Pair<ImageBitmap?, Boolean>>(
        initialValue = null to app.hasBanner,
        key1 = app.packageName,
        key2 = app.activityName,
    ) {
        value = iconLoader.loadBannerOrIcon(app)
    }

    val cardWidth = if (featured) 204.dp else 184.dp
    val mediaHeight = if (featured) 100.dp else 88.dp
    val shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)
    val cardDescription = stringResource(R.string.cd_app_icon, app.label)
    val primaryAccent = if (pickerMode && isFavorite) palette.accentPrimary else if (isFocused) palette.accentPrimary else palette.accentSecondary

    Surface(
        onClick = onClick,
        onLongClick = onOpenContextMenu,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.08f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            isSelected = pickerMode && isFavorite,
        ),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 16.dp),
        modifier = modifier
            .width(cardWidth)
            .semantics { contentDescription = cardDescription }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU ||
                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_INFO)
                ) {
                    onOpenContextMenu()
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawCyberHudBrackets(
                    isFocused = isFocused,
                    isSelected = pickerMode && isFavorite,
                    primaryColor = palette.accentPrimary,
                    secondaryColor = palette.accentSecondary,
                )
                .padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Top laser telemetry strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    palette.accentPrimary.copy(alpha = if (isFocused) 1f else 0.45f),
                                    palette.accentSecondary.copy(alpha = if (isFocused) 0.85f else 0.15f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.45f)),
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) palette.accentSecondary else palette.textSecondary.copy(alpha = 0.35f)),
                )
            }

            // Holographic media viewport
            val viewportShape = CutCornerShape(topStart = 9.dp, bottomEnd = 9.dp, topEnd = 2.dp, bottomStart = 2.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mediaHeight)
                    .clip(viewportShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.backgroundStart,
                                palette.backgroundEnd,
                                palette.accentPrimary.copy(alpha = if (isFocused) 0.24f else 0.12f),
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = palette.accentPrimary.copy(alpha = if (isFocused) 0.55f else 0.22f),
                        shape = viewportShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val (bitmap, isBanner) = iconState

                // Subtle holographic scanlines & pedestal rings for non-banner apps
                if (bitmap == null || !isBanner) {
                    CyberHoloPedestalCanvas(
                        accentPrimary = palette.accentPrimary,
                        accentSecondary = palette.accentSecondary,
                        isFocused = isFocused,
                    )
                }

                when {
                    bitmap != null && isBanner -> {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Subtle bottom HUD vignette over banner
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.42f),
                                        ),
                                    ),
                                ),
                        )
                    }

                    bitmap != null -> {
                        Box(
                            modifier = Modifier
                                .size(if (featured) 60.dp else 52.dp)
                                .clip(CutCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(
                                    width = 1.dp,
                                    color = palette.accentPrimary.copy(alpha = if (isFocused) 0.8f else 0.38f),
                                    shape = CutCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                            )
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CutCornerShape(10.dp))
                                .background(palette.accentPrimary.copy(alpha = 0.20f))
                                .border(
                                    width = 1.dp,
                                    color = palette.accentPrimary,
                                    shape = CutCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = app.label.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.accentPrimary,
                                maxLines = 1,
                            )
                        }
                    }
                }

                // Top-end HUD badge (Picker selection status OR TV Leanback badge)
                if (pickerMode) {
                    val badgeBg = if (isFavorite) {
                        palette.accentPrimary
                    } else {
                        Color.Black.copy(alpha = 0.78f)
                    }
                    val badgeFg = if (isFavorite) {
                        Color.Black
                    } else {
                        palette.textPrimary
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                            .background(badgeBg)
                            .border(
                                width = 1.dp,
                                color = if (isFavorite) Color.White.copy(alpha = 0.7f) else palette.accentPrimary.copy(alpha = 0.5f),
                                shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (isFavorite) R.string.app_picker_badge_selected else R.string.app_picker_badge_add,
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeFg,
                            maxLines = 1,
                        )
                    }
                } else if (app.isLeanback) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .clip(CutCornerShape(topStart = 5.dp, bottomEnd = 5.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .border(
                                width = 1.dp,
                                color = palette.accentPrimary.copy(alpha = 0.65f),
                                shape = CutCornerShape(topStart = 5.dp, bottomEnd = 5.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_badge_leanback),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Bottom high-contrast HUD nameplate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isFocused) {
                            palette.accentPrimary.copy(alpha = 0.18f)
                        } else {
                            Color.Black.copy(alpha = 0.45f)
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LeadingFocusIndicator(
                    isFocused = isFocused,
                    palette = palette,
                    accentColor = primaryAccent,
                    height = 16.dp,
                )
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isFavorite) {
                    Icon(
                        painter = painterResource(if (pickerMode) R.drawable.ic_neon_check else R.drawable.ic_neon_star),
                        contentDescription = stringResource(R.string.home_badge_favorite),
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * Futuristic HUD action card displayed on the Home screen so the user can pick which
 * apps appear on their main menu with a single press of OK.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChooseAppsHudCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = true,
) {
    val palette = LocalNeonPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val cardWidth = if (featured) 204.dp else 184.dp
    val mediaHeight = if (featured) 100.dp else 88.dp
    val shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.08f),
        border = NeonFocusEffects.cardBorder(
            palette = palette,
            shape = shape,
            isSelected = isFocused,
            accentStart = palette.accentPrimary,
            accentEnd = palette.accentSecondary,
        ),
        glow = NeonFocusEffects.cardGlow(palette, elevation = 16.dp),
        modifier = modifier.width(cardWidth),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawCyberHudBrackets(
                    isFocused = isFocused,
                    isSelected = true,
                    primaryColor = palette.accentPrimary,
                    secondaryColor = palette.accentSecondary,
                )
                .padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(palette.accentPrimary, palette.accentSecondary),
                            ),
                        ),
                )
            }

            val viewportShape = CutCornerShape(topStart = 9.dp, bottomEnd = 9.dp, topEnd = 2.dp, bottomStart = 2.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mediaHeight)
                    .clip(viewportShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.backgroundStart,
                                palette.accentPrimary.copy(alpha = if (isFocused) 0.28f else 0.16f),
                                palette.backgroundEnd,
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = palette.accentPrimary.copy(alpha = 0.65f),
                        shape = viewportShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CyberHoloPedestalCanvas(
                    accentPrimary = palette.accentPrimary,
                    accentSecondary = palette.accentSecondary,
                    isFocused = isFocused,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CutCornerShape(8.dp))
                            .background(palette.accentPrimary.copy(alpha = 0.22f))
                            .border(1.5.dp, palette.accentPrimary, CutCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.accentPrimary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_add_apps_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isFocused) {
                            palette.accentPrimary.copy(alpha = 0.24f)
                        } else {
                            Color.Black.copy(alpha = 0.45f)
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LeadingFocusIndicator(
                    isFocused = isFocused,
                    palette = palette,
                    height = 16.dp,
                )
                Text(
                    text = stringResource(R.string.home_add_apps_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CyberHoloPedestalCanvas(
    accentPrimary: Color,
    accentSecondary: Color,
    isFocused: Boolean,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)

        // Horizontal scanlines
        val scanStep = 7.dp.toPx()
        var y = scanStep
        while (y < h) {
            drawLine(
                color = accentPrimary.copy(alpha = if (isFocused) 0.09f else 0.05f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
            )
            y += scanStep
        }

        // Dual concentric hologram rings behind icon
        val outerR = minOf(w, h) * 0.42f
        val innerR = minOf(w, h) * 0.30f
        drawCircle(
            color = accentPrimary.copy(alpha = if (isFocused) 0.42f else 0.22f),
            radius = outerR,
            center = center,
            style = Stroke(width = 1.4f),
        )
        drawCircle(
            color = accentSecondary.copy(alpha = if (isFocused) 0.35f else 0.16f),
            radius = innerR,
            center = center,
            style = Stroke(width = 1.1f),
        )

        // Subtle crosshair ticks
        val tickLen = 8.dp.toPx()
        val tickColor = accentPrimary.copy(alpha = if (isFocused) 0.55f else 0.25f)
        drawLine(tickColor, Offset(center.x - outerR - tickLen, center.y), Offset(center.x - outerR, center.y), 1.3f)
        drawLine(tickColor, Offset(center.x + outerR, center.y), Offset(center.x + outerR + tickLen, center.y), 1.3f)
    }
}

/**
 * Draws glowing Cyber-HUD corner reticles (`┌ ┐ └ ┘`) on the tile frame.
 */
private fun Modifier.drawCyberHudBrackets(
    isFocused: Boolean,
    isSelected: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
): Modifier = this.drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    val arm = if (isFocused) 14.dp.toPx() else 10.dp.toPx()
    val inset = 3.dp.toPx()
    val stroke = if (isFocused) 2.4.dp.toPx() else 1.4.dp.toPx()
    val topColor = when {
        isFocused -> primaryColor
        isSelected -> primaryColor.copy(alpha = 0.85f)
        else -> primaryColor.copy(alpha = 0.42f)
    }
    val bottomColor = when {
        isFocused -> secondaryColor
        isSelected -> primaryColor.copy(alpha = 0.85f)
        else -> secondaryColor.copy(alpha = 0.36f)
    }

    // Top-left reticle
    drawLine(topColor, Offset(inset, inset), Offset(inset + arm, inset), stroke)
    drawLine(topColor, Offset(inset, inset), Offset(inset, inset + arm), stroke)

    // Top-right reticle
    drawLine(topColor, Offset(w - inset - arm, inset), Offset(w - inset, inset), stroke)
    drawLine(topColor, Offset(w - inset, inset), Offset(w - inset, inset + arm), stroke)

    // Bottom-left reticle
    drawLine(bottomColor, Offset(inset, h - inset - arm), Offset(inset, h - inset), stroke)
    drawLine(bottomColor, Offset(inset, h - inset), Offset(inset + arm, h - inset), stroke)

    // Bottom-right reticle
    drawLine(bottomColor, Offset(w - inset - arm, h - inset), Offset(w - inset, h - inset), stroke)
    drawLine(bottomColor, Offset(w - inset, h - inset - arm), Offset(w - inset, h - inset), stroke)
}
