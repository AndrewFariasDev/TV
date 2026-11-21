package dev.neonlauncher.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
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
import dev.neonlauncher.core.AppIconLoader
import dev.neonlauncher.data.AppInfo
import dev.neonlauncher.ui.theme.LocalNeonPalette

/**
 * Glassmorphic TV app tile displaying either an Android TV banner or a centered
 * app icon inside a neon-accented plate, with D-pad long-press and Menu/Info support.
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

    val cardWidth = if (featured) 196.dp else 168.dp
    val mediaHeight = if (featured) 96.dp else 82.dp
    val shape = RoundedCornerShape(16.dp)
    val cardDescription = stringResource(R.string.cd_app_icon, app.label)

    Surface(
        onClick = onClick,
        onLongClick = onOpenContextMenu,
        interactionSource = interactionSource,
        shape = NeonFocusEffects.cardShape(shape),
        colors = NeonFocusEffects.cardColors(palette),
        scale = NeonFocusEffects.cardScale(palette.performanceMode, focusedScale = 1.08f),
        border = NeonFocusEffects.cardBorder(palette, shape),
        glow = NeonFocusEffects.cardGlow(palette),
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
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mediaHeight)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                palette.backgroundStart.copy(alpha = 0.85f),
                                palette.accentTertiary.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val (bitmap, isBanner) = iconState
                when {
                    bitmap != null && isBanner -> {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    bitmap != null -> {
                        Box(
                            modifier = Modifier
                                .size(if (featured) 58.dp else 50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.accentPrimary.copy(alpha = 0.18f)),
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

                if (app.isLeanback) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.58f))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LeadingFocusIndicator(
                    isFocused = isFocused,
                    palette = palette,
                    height = 18.dp,
                )
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) palette.textPrimary else palette.textPrimary.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isFavorite) {
                    Icon(
                        painter = painterResource(R.drawable.ic_neon_star),
                        contentDescription = stringResource(R.string.home_badge_favorite),
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
