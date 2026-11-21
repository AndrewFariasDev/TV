package dev.neonlauncher.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Inspects hardware, OS API level, and Home/Dream capabilities cleanly without
 * brand or model hardcoding.
 */
object DeviceCapabilitiesProvider {
    private const val LOW_RAM_BYTES_THRESHOLD = 1_500_000_000L

    fun inspect(context: Context): DeviceCapabilities {
        val pm = context.packageManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRamDevice = activityManager?.isLowRamDevice == true
        val totalMem = readTotalMemoryBytes(activityManager)
        val isLowPerformanceByDefault =
            (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 && isLowRamDevice) ||
                (totalMem in 1L..900_000_000L) ||
                Runtime.getRuntime().availableProcessors() <= 2

        val hasLeanbackFeature = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isLowRamDevice
        val supportsRoleManager = HomeAliasManager.supportsRoleManager(context)
        val supportsDreamService = pm.resolveActivityCompat(
            Intent(Settings.ACTION_DREAM_SETTINGS),
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null

        return DeviceCapabilities(
            sdkInt = Build.VERSION.SDK_INT,
            androidRelease = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
            isLowRamDevice = isLowRamDevice,
            isLowPerformanceByDefault = isLowPerformanceByDefault,
            hasLeanbackFeature = hasLeanbackFeature,
            supportsBlur = supportsBlur,
            supportsRoleManager = supportsRoleManager,
            supportsDreamService = supportsDreamService,
            isDefaultHome = HomeAliasManager.isDefaultHome(context),
            isHomeAliasEnabled = HomeAliasManager.isHomeAliasEnabled(context),
        )
    }

    private fun readTotalMemoryBytes(activityManager: ActivityManager?): Long {
        if (activityManager == null) return 0L
        return runCatching {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            info.totalMem
        }.getOrDefault(0L)
    }
}
