package dev.neonlauncher.core

import androidx.compose.ui.graphics.ImageBitmap
import dev.neonlauncher.data.AppInfo

/**
 * Snapshot of platform capabilities and Home-role status for the current TV device.
 */
data class DeviceCapabilities(
    val sdkInt: Int,
    val androidRelease: String,
    val isLowRamDevice: Boolean,
    val isLowPerformanceByDefault: Boolean,
    val hasLeanbackFeature: Boolean,
    val supportsBlur: Boolean,
    val supportsRoleManager: Boolean,
    val supportsDreamService: Boolean,
    val isDefaultHome: Boolean,
    val isHomeAliasEnabled: Boolean,
)

/**
 * Asynchronous loader for app banners and icons returned as `(bitmap, isBanner)`.
 */
interface AppIconLoader {
    suspend fun loadBannerOrIcon(app: AppInfo): Pair<ImageBitmap?, Boolean>
}
