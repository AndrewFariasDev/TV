package dev.neonlauncher.core

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * Version-safe wrappers over [PackageManager] intent query and resolution APIs.
 */
internal fun PackageManager.queryIntentActivitiesCompat(
    intent: Intent,
    flags: Int = 0,
): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, flags)
    }
}

internal fun PackageManager.resolveActivityCompat(
    intent: Intent,
    flags: Int = 0,
): ResolveInfo? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        resolveActivity(intent, flags)
    }
}
