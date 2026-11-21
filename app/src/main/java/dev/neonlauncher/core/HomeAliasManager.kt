package dev.neonlauncher.core

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Manages the `.HomeAlias` manifest component, default-Home detection,
 * RoleManager / Home-chooser intents, and returning to the stock launcher.
 */
object HomeAliasManager {
    private const val HOME_ALIAS_SUFFIX = ".HomeAlias"

    private fun homeAliasComponent(context: Context): ComponentName =
        ComponentName(context.packageName, "${context.packageName}$HOME_ALIAS_SUFFIX")

    private fun createHomeIntent(): Intent =
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** Enables or disables the `.HomeAlias` component synchronously without killing the process. */
    fun setHomeAliasEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            homeAliasComponent(context),
            state,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** Returns `true` when `.HomeAlias` is currently enabled in [PackageManager]. */
    fun isHomeAliasEnabled(context: Context): Boolean {
        val state = context.packageManager.getComponentEnabledSetting(homeAliasComponent(context))
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }

    /** Returns `true` when Neon Launcher is currently the system's active default HOME handler. */
    fun isDefaultHome(context: Context): Boolean {
        if (!isHomeAliasEnabled(context)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return true
            }
        }
        val resolved = context.packageManager.resolveActivityCompat(
            createHomeIntent(),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** Returns `true` when the device supports `RoleManager.ROLE_HOME` on API 29+. */
    fun supportsRoleManager(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_HOME)
    }

    /**
     * Builds the best resolved [Intent] for assigning Neon Launcher as default Home:
     * `RoleManager.ROLE_HOME` on API 29+ when available, otherwise `ACTION_HOME_SETTINGS`.
     * Never uses `Intent.createChooser(CATEGORY_HOME)` because on Android TV that re-launches
     * `MainActivity` itself without persisting a default Home preference.
     */
    fun createSetHomeIntent(context: Context): Intent? {
        if (!isHomeAliasEnabled(context)) {
            setHomeAliasEnabled(context, true)
        }
        if (isDefaultHome(context)) {
            return null
        }
        val pm = context.packageManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                if (pm.resolveActivityCompat(roleIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    return roleIntent
                }
            }
        }

        val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS)
        if (pm.resolveActivityCompat(homeSettingsIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            return homeSettingsIntent
        }

        return null
    }

    /**
     * Disables `.HomeAlias` and navigates to the stock/next available Home launcher
     * (or `Settings.ACTION_HOME_SETTINGS` if no external Home activity can be launched directly).
     */
    fun returnToStockLauncher(context: Context): Boolean {
        setHomeAliasEnabled(context, false)
        val pm = context.packageManager
        val candidates = pm.queryIntentActivitiesCompat(
            createHomeIntent(),
            PackageManager.MATCH_DEFAULT_ONLY,
        ).filter { info ->
            val pkg = info.activityInfo?.packageName
            pkg != null && pkg != context.packageName && pkg != "android"
        }

        val preferred = candidates.firstOrNull { info ->
            (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } ?: candidates.firstOrNull()

        if (preferred != null) {
            val launchIntent = createHomeIntent().apply {
                setClassName(preferred.activityInfo.packageName, preferred.activityInfo.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (pm.resolveActivityCompat(launchIntent) != null) {
                val started = runCatching { context.startActivity(launchIntent) }.isSuccess
                if (started) return true
            }
        }

        val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (pm.resolveActivityCompat(homeSettingsIntent) != null) {
            return runCatching { context.startActivity(homeSettingsIntent) }.isSuccess
        }

        return false
    }
}
