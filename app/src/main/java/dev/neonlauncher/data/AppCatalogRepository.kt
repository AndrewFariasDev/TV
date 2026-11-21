package dev.neonlauncher.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.neonlauncher.core.AppIconLoader
import dev.neonlauncher.core.queryIntentActivitiesCompat
import dev.neonlauncher.core.resolveActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Discovers Leanback and standard launchable applications, resolves app management
 * intents, and caches memory-bounded banner/icon [ImageBitmap]s on [Dispatchers.IO].
 */
class AppCatalogRepository(context: Context) : AppIconLoader {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val bitmapCache: LruCache<String, Pair<ImageBitmap, Boolean>>

    init {
        val isLowRam = (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.isLowRamDevice == true
        val cacheSizeKb = if (isLowRam) LOW_RAM_CACHE_KB else STANDARD_CACHE_KB
        bitmapCache = object : LruCache<String, Pair<ImageBitmap, Boolean>>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Pair<ImageBitmap, Boolean>): Int {
                val bmp = value.first
                return maxOf(1, (bmp.width * bmp.height * 4) / 1024)
            }
        }
    }

    /**
     * Queries Leanback and standard launcher activities, merging duplicates by package
     * with Leanback activities taking priority.
     */
    suspend fun loadApps(recentLaunchTimes: Map<String, Long> = emptyMap()): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val appsByPackage = LinkedHashMap<String, AppInfo>()

            val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            for (info in packageManager.queryIntentActivitiesCompat(leanbackIntent)) {
                val activityInfo = info.activityInfo ?: continue
                val pkg = activityInfo.packageName ?: continue
                if (pkg == appContext.packageName) continue
                if (appsByPackage.containsKey(pkg)) continue

                appsByPackage[pkg] = buildAppInfo(
                    resolveLabel = info.loadLabel(packageManager)?.toString(),
                    packageName = pkg,
                    activityName = activityInfo.name,
                    isLeanback = true,
                    hasBanner = activityInfo.bannerResource != 0 || activityInfo.applicationInfo.banner != 0,
                    applicationInfo = activityInfo.applicationInfo,
                    lastLaunchedAt = recentLaunchTimes[pkg] ?: 0L,
                )
            }

            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            for (info in packageManager.queryIntentActivitiesCompat(launcherIntent)) {
                val activityInfo = info.activityInfo ?: continue
                val pkg = activityInfo.packageName ?: continue
                if (pkg == appContext.packageName) continue
                if (appsByPackage.containsKey(pkg)) continue

                appsByPackage[pkg] = buildAppInfo(
                    resolveLabel = info.loadLabel(packageManager)?.toString(),
                    packageName = pkg,
                    activityName = activityInfo.name,
                    isLeanback = false,
                    hasBanner = activityInfo.bannerResource != 0 || activityInfo.applicationInfo.banner != 0,
                    applicationInfo = activityInfo.applicationInfo,
                    lastLaunchedAt = recentLaunchTimes[pkg] ?: 0L,
                )
            }

            appsByPackage.values.toList()
        }

    override suspend fun loadBannerOrIcon(app: AppInfo): Pair<ImageBitmap?, Boolean> {
        val key = "${app.packageName}/${app.activityName}"
        bitmapCache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            bitmapCache.get(key)?.let { return@withContext it }

            val component = ComponentName(app.packageName, app.activityName)
            var loaded: Pair<ImageBitmap, Boolean>? = null

            if (app.hasBanner) {
                val bannerDrawable = runCatching { packageManager.getActivityBanner(component) }.getOrNull()
                    ?: runCatching { packageManager.getApplicationBanner(app.packageName) }.getOrNull()
                if (bannerDrawable != null) {
                    renderBoundedBitmap(bannerDrawable, MAX_BANNER_WIDTH, MAX_BANNER_HEIGHT)?.let {
                        loaded = it to true
                    }
                }
            }

            if (loaded == null) {
                val iconDrawable = runCatching { packageManager.getActivityIcon(component) }.getOrNull()
                    ?: runCatching { packageManager.getApplicationIcon(app.packageName) }.getOrNull()
                if (iconDrawable != null) {
                    renderBoundedBitmap(iconDrawable, MAX_ICON_SIZE, MAX_ICON_SIZE)?.let {
                        loaded = it to false
                    }
                }
            }

            val result = loaded
            if (result != null) {
                bitmapCache.put(key, result)
                result
            } else {
                null to false
            }
        }
    }

    /** Clears cached bitmaps when packages change or memory pressure occurs. */
    fun clearCache() {
        bitmapCache.evictAll()
    }

    /** Builds a verified launch [Intent] for [app], or `null` if unavailable. */
    fun createLaunchIntent(app: AppInfo): Intent? {
        val category = if (app.isLeanback) {
            Intent.CATEGORY_LEANBACK_LAUNCHER
        } else {
            Intent.CATEGORY_LAUNCHER
        }
        val explicit = Intent(Intent.ACTION_MAIN).apply {
            addCategory(category)
            component = ComponentName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        if (packageManager.resolveActivityCompat(explicit) != null) {
            return explicit
        }

        val fallback = packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: packageManager.getLaunchIntentForPackage(app.packageName)
        return fallback?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }?.takeIf { packageManager.resolveActivityCompat(it) != null }
    }

    /** Builds a verified App Info settings [Intent] for [packageName], or `null`. */
    fun createAppInfoIntent(packageName: String): Intent? {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent.takeIf { packageManager.resolveActivityCompat(it) != null }
    }

    /** Builds a verified uninstall [Intent] for [packageName], or `null`. */
    fun createUninstallIntent(packageName: String): Intent? {
        val pkgUri = Uri.fromParts("package", packageName, null)
        val deleteIntent = Intent(Intent.ACTION_DELETE, pkgUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (packageManager.resolveActivityCompat(deleteIntent) != null) {
            return deleteIntent
        }

        @Suppress("DEPRECATION")
        val legacyUninstall = Intent(Intent.ACTION_UNINSTALL_PACKAGE, pkgUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return legacyUninstall.takeIf { packageManager.resolveActivityCompat(it) != null }
    }

    private fun buildAppInfo(
        resolveLabel: String?,
        packageName: String,
        activityName: String,
        isLeanback: Boolean,
        hasBanner: Boolean,
        applicationInfo: ApplicationInfo,
        lastLaunchedAt: Long,
    ): AppInfo {
        val label = resolveLabel?.trim()?.takeIf { it.isNotEmpty() } ?: packageName
        val isSystem = (applicationInfo.flags and
            (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        return AppInfo(
            packageName = packageName,
            activityName = activityName,
            label = label,
            isLeanback = isLeanback,
            isSystemApp = isSystem,
            hasBanner = hasBanner,
            lastLaunchedAt = lastLaunchedAt,
        )
    }

    private fun renderBoundedBitmap(
        drawable: Drawable,
        maxWidth: Int,
        maxHeight: Int,
    ): ImageBitmap? {
        return runCatching {
            val intrinsicW = drawable.intrinsicWidth
            val intrinsicH = drawable.intrinsicHeight
            val (targetW, targetH) = if (intrinsicW > 0 && intrinsicH > 0) {
                val scale = min(
                    1f,
                    min(maxWidth.toFloat() / intrinsicW, maxHeight.toFloat() / intrinsicH),
                )
                maxOf(1, (intrinsicW * scale).roundToInt()) to maxOf(1, (intrinsicH * scale).roundToInt())
            } else {
                maxWidth to maxHeight
            }
            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val oldBounds = drawable.copyBounds()
            drawable.setBounds(0, 0, targetW, targetH)
            drawable.draw(canvas)
            drawable.bounds = oldBounds
            bitmap.asImageBitmap()
        }.getOrNull()
    }

    private companion object {
        const val MAX_BANNER_WIDTH = 320
        const val MAX_BANNER_HEIGHT = 180
        const val MAX_ICON_SIZE = 128
        const val LOW_RAM_CACHE_KB = 4 * 1024
        const val STANDARD_CACHE_KB = 16 * 1024
    }
}
