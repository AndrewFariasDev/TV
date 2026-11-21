package dev.neonlauncher.data

import androidx.annotation.StringRes

/** Background visual styles supported by Neon Launcher themes and settings. */
enum class BackgroundStyle {
    AURORA,
    GRID,
    CYBER_PULSE,
    SOLID,
}

/** Ordering modes for the Home app catalog and Favorites row. */
enum class SortOrder {
    ALPHABETICAL,
    RECENT,
}

/**
 * ARGB color palette (stored as 32-bit unsigned 0xAARRGGBB values in Long)
 * shared between core theme loading and Jetpack Compose UI rendering.
 */
data class ThemeColors(
    val backgroundStart: Long,
    val backgroundEnd: Long,
    val surface: Long,
    val surfaceFocused: Long,
    val surfaceBorder: Long,
    val accentPrimary: Long,
    val accentSecondary: Long,
    val accentTertiary: Long,
    val textPrimary: Long,
    val textSecondary: Long,
    val danger: Long,
)

/** Theme configuration loaded from bundled JSON assets or the built-in fallback. */
data class NeonThemeConfig(
    val id: String,
    val name: String,
    val nameEs: String?,
    val author: String,
    val backgroundStyle: BackgroundStyle,
    val colors: ThemeColors,
)

/** Launchable application metadata discovered from PackageManager. */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
    val isSystemApp: Boolean,
    val hasBanner: Boolean,
    val lastLaunchedAt: Long = 0L,
)

/** Device-level system shortcuts exposed in the Quick Controls bar. */
enum class QuickActionType {
    SETTINGS,
    WIFI,
    BLUETOOTH,
    DISPLAY,
    TV_INPUTS,
    SCREENSAVER,
}

/** Resolved Quick Control item guaranteed to have a target on the current device. */
data class QuickAction(
    val type: QuickActionType,
    @param:StringRes val labelRes: Int,
    val subtitle: String? = null,
)
