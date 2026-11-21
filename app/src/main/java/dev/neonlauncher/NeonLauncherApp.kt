package dev.neonlauncher

import android.app.Application
import dev.neonlauncher.core.SafeModeManager
import dev.neonlauncher.data.AppCatalogRepository
import dev.neonlauncher.data.PreferencesRepository
import dev.neonlauncher.data.ThemeRepository

/**
 * Application entry point installing Safe Mode crash protection before UI creation
 * and providing lightweight shared repositories.
 */
class NeonLauncherApp : Application() {
    val themeRepository: ThemeRepository by lazy { ThemeRepository(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val appCatalogRepository: AppCatalogRepository by lazy { AppCatalogRepository(this) }

    override fun onCreate() {
        super.onCreate()
        SafeModeManager.install(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            appCatalogRepository.clearCache()
        }
    }
}
