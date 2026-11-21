package dev.neonlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings
import dev.neonlauncher.R
import dev.neonlauncher.core.resolveActivityCompat

/**
 * Discovers supported system Quick Actions by probing hardware features and
 * verifying intent resolution before exposing any shortcut.
 */
object QuickActionsResolver {
    private val homeQuickActionTypes = listOf(
        QuickActionType.WIFI,
        QuickActionType.TV_INPUTS,
    )

    fun resolveAvailableActions(context: Context): List<QuickAction> {
        return buildList {
            for (type in homeQuickActionTypes) {
                if (resolveIntent(context, type) != null) {
                    add(QuickAction(type = type, labelRes = labelResFor(type)))
                }
            }
        }
    }

    /**
     * Resolves a launchable [Intent] for [type], or returns `null` when the
     * capability or target Activity is unavailable on this device.
     */
    fun resolveIntent(context: Context, type: QuickActionType): Intent? {
        val pm = context.packageManager
        val candidates = when (type) {
            QuickActionType.SETTINGS -> listOf(Intent(Settings.ACTION_SETTINGS))
            QuickActionType.WIFI -> listOf(
                Intent(Settings.ACTION_WIFI_SETTINGS),
                Intent(Settings.ACTION_WIRELESS_SETTINGS),
            )
            QuickActionType.BLUETOOTH -> {
                if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) return null
                listOf(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            QuickActionType.DISPLAY -> listOf(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            QuickActionType.TV_INPUTS -> {
                if (!hasActiveTvInputs(context, pm)) return null
                buildList {
                    add(Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        add(Intent(TvInputManager.ACTION_SETUP_INPUTS))
                    }
                }
            }
            QuickActionType.SCREENSAVER -> listOf(Intent(Settings.ACTION_DREAM_SETTINGS))
        }

        return candidates
            .asSequence()
            .map { it.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } }
            .firstOrNull { pm.resolveActivityCompat(it, PackageManager.MATCH_DEFAULT_ONLY) != null }
    }

    private fun hasActiveTvInputs(context: Context, pm: PackageManager): Boolean {
        if (!pm.hasSystemFeature(PackageManager.FEATURE_LIVE_TV)) return false
        val tvInputManager = context.getSystemService(TvInputManager::class.java) ?: return false
        return runCatching {
            val inputs = tvInputManager.tvInputList
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                inputs.any { input -> !input.isHidden(context) }
            } else {
                inputs.isNotEmpty()
            }
        }.getOrDefault(false)
    }

    private fun labelResFor(type: QuickActionType): Int = when (type) {
        QuickActionType.SETTINGS -> R.string.quick_action_settings
        QuickActionType.WIFI -> R.string.quick_action_wifi
        QuickActionType.BLUETOOTH -> R.string.quick_action_bluetooth
        QuickActionType.DISPLAY -> R.string.quick_action_display
        QuickActionType.TV_INPUTS -> R.string.quick_action_tv_inputs
        QuickActionType.SCREENSAVER -> R.string.quick_action_screensaver
    }
}
