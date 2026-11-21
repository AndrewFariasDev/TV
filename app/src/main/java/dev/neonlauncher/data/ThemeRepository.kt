package dev.neonlauncher.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads theme configurations from `assets/themes/` JSON files with guaranteed
 * fallback to [ThemeJsonParser.CyberCyanFallback].
 */
class ThemeRepository(private val context: Context) {
    /**
     * Reads and parses bundled theme files on [Dispatchers.IO].
     * Always includes `cyber-cyan` sorted first so the launcher always has a valid theme.
     */
    suspend fun loadThemes(): List<NeonThemeConfig> = withContext(Dispatchers.IO) {
        val loaded = linkedMapOf<String, NeonThemeConfig>()
        val assetManager = context.assets
        val files = runCatching { assetManager.list(THEMES_ASSET_DIR)?.toList().orEmpty() }
            .getOrDefault(emptyList())

        for (fileName in files.sorted()) {
            if (!fileName.endsWith(".json", ignoreCase = true)) continue
            val parsed = runCatching {
                assetManager.open("$THEMES_ASSET_DIR/$fileName").bufferedReader().use { reader ->
                    ThemeJsonParser.parseOrNull(reader.readText())
                }
            }.getOrNull()
            if (parsed != null) {
                loaded[parsed.id] = parsed
            }
        }

        if (!loaded.containsKey(ThemeJsonParser.DEFAULT_THEME_ID)) {
            loaded[ThemeJsonParser.DEFAULT_THEME_ID] = ThemeJsonParser.CyberCyanFallback
        }

        val defaultTheme = loaded[ThemeJsonParser.DEFAULT_THEME_ID]!!
        val remaining = loaded.values
            .filterNot { it.id == ThemeJsonParser.DEFAULT_THEME_ID }
            .sortedBy { it.name.lowercase() }

        listOf(defaultTheme) + remaining
    }

    /** Resolves [themeId] against [themes], falling back to the default theme. */
    fun resolveTheme(themes: List<NeonThemeConfig>, themeId: String?): NeonThemeConfig {
        if (!themeId.isNullOrBlank()) {
            themes.firstOrNull { it.id == themeId }?.let { return it }
        }
        return themes.firstOrNull { it.id == ThemeJsonParser.DEFAULT_THEME_ID }
            ?: themes.firstOrNull()
            ?: ThemeJsonParser.CyberCyanFallback
    }

    private companion object {
        const val THEMES_ASSET_DIR = "themes"
    }
}
