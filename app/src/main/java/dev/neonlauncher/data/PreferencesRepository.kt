package dev.neonlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.neonPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "neon_launcher_prefs",
)

/** Persisted user preferences snapshot from Jetpack DataStore. */
data class UserPreferences(
    val themeId: String = ThemeJsonParser.DEFAULT_THEME_ID,
    val backgroundStyleOverride: BackgroundStyle? = null,
    val favoritePackages: Set<String> = emptySet(),
    val hiddenPackages: Set<String> = emptySet(),
    val recentLaunchTimes: Map<String, Long> = emptyMap(),
    val sortOrder: SortOrder = SortOrder.ALPHABETICAL,
    val use24HourClock: Boolean = true,
    val performanceModeOverride: Boolean? = null,
    val highContrastMode: Boolean = false,
    val onboardingCompleted: Boolean = false,
)

/**
 * Manages Neon Launcher settings and launcher state in DataStore Preferences.
 */
class PreferencesRepository(private val context: Context) {
    private val dataStore = context.applicationContext.neonPreferencesDataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toUserPreferences() }

    suspend fun setThemeId(themeId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_ID] = themeId
            // Selecting a theme clears a manual style override so the theme's native style applies.
            prefs.remove(KEY_BACKGROUND_STYLE_OVERRIDE)
        }
    }

    suspend fun setBackgroundStyleOverride(style: BackgroundStyle?) {
        dataStore.edit { prefs ->
            if (style == null) {
                prefs.remove(KEY_BACKGROUND_STYLE_OVERRIDE)
            } else {
                prefs[KEY_BACKGROUND_STYLE_OVERRIDE] = style.name
            }
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES].orEmpty().toMutableSet()
            if (!current.add(packageName)) {
                current.remove(packageName)
            }
            prefs[KEY_FAVORITES] = current
        }
    }

    suspend fun hideApp(packageName: String) {
        dataStore.edit { prefs ->
            val hidden = prefs[KEY_HIDDEN_APPS].orEmpty().toMutableSet()
            hidden.add(packageName)
            prefs[KEY_HIDDEN_APPS] = hidden
        }
    }

    suspend fun unhideApp(packageName: String) {
        dataStore.edit { prefs ->
            val hidden = prefs[KEY_HIDDEN_APPS].orEmpty().toMutableSet()
            hidden.remove(packageName)
            prefs[KEY_HIDDEN_APPS] = hidden
        }
    }

    suspend fun recordLaunch(packageName: String, timestampMs: Long) {
        dataStore.edit { prefs ->
            val current = decodeRecentLaunches(prefs[KEY_RECENT_LAUNCHES].orEmpty()).toMutableMap()
            current[packageName] = timestampMs
            prefs[KEY_RECENT_LAUNCHES] = encodeRecentLaunches(current)
        }
    }

    suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.edit { prefs ->
            prefs[KEY_SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun setUse24HourClock(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_USE_24H_CLOCK] = enabled
        }
    }

    suspend fun setPerformanceModeOverride(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PERFORMANCE_MODE_OVERRIDE] = enabled
        }
    }

    suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HIGH_CONTRAST_MODE] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun resetSettings(keepOnboardingCompleted: Boolean = true) {
        dataStore.edit { prefs ->
            val onboarding = prefs[KEY_ONBOARDING_COMPLETED] ?: false
            prefs.clear()
            if (keepOnboardingCompleted && onboarding) {
                prefs[KEY_ONBOARDING_COMPLETED] = true
            }
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val styleOverride = this[KEY_BACKGROUND_STYLE_OVERRIDE]?.let { raw ->
            BackgroundStyle.entries.firstOrNull { it.name == raw }
        }
        val sortOrder = this[KEY_SORT_ORDER]?.let { raw ->
            SortOrder.entries.firstOrNull { it.name == raw }
        } ?: SortOrder.ALPHABETICAL

        return UserPreferences(
            themeId = this[KEY_THEME_ID] ?: ThemeJsonParser.DEFAULT_THEME_ID,
            backgroundStyleOverride = styleOverride,
            favoritePackages = this[KEY_FAVORITES].orEmpty(),
            hiddenPackages = this[KEY_HIDDEN_APPS].orEmpty(),
            recentLaunchTimes = decodeRecentLaunches(this[KEY_RECENT_LAUNCHES].orEmpty()),
            sortOrder = sortOrder,
            use24HourClock = this[KEY_USE_24H_CLOCK] ?: true,
            performanceModeOverride = this[KEY_PERFORMANCE_MODE_OVERRIDE],
            highContrastMode = this[KEY_HIGH_CONTRAST_MODE] ?: false,
            onboardingCompleted = this[KEY_ONBOARDING_COMPLETED] ?: false,
        )
    }

    private fun decodeRecentLaunches(entries: Set<String>): Map<String, Long> {
        if (entries.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, Long>(entries.size)
        for (entry in entries) {
            val idx = entry.lastIndexOf('=')
            if (idx <= 0 || idx >= entry.lastIndex) continue
            val pkg = entry.substring(0, idx)
            val ts = entry.substring(idx + 1).toLongOrNull() ?: continue
            if (ts > 0L) {
                result[pkg] = ts
            }
        }
        return result
    }

    private fun encodeRecentLaunches(map: Map<String, Long>): Set<String> {
        return map.entries
            .asSequence()
            .filter { it.value > 0L }
            .sortedByDescending { it.value }
            .take(MAX_RECENT_ENTRIES)
            .map { "${it.key}=${it.value}" }
            .toSet()
    }

    private companion object {
        const val MAX_RECENT_ENTRIES = 128

        val KEY_THEME_ID = stringPreferencesKey("theme_id")
        val KEY_BACKGROUND_STYLE_OVERRIDE = stringPreferencesKey("background_style_override")
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_packages")
        val KEY_HIDDEN_APPS = stringSetPreferencesKey("hidden_packages")
        val KEY_RECENT_LAUNCHES = stringSetPreferencesKey("recent_launches")
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        val KEY_USE_24H_CLOCK = booleanPreferencesKey("use_24h_clock")
        val KEY_PERFORMANCE_MODE_OVERRIDE = booleanPreferencesKey("performance_mode_override")
        val KEY_HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
