package dev.neonlauncher.data

import org.json.JSONObject

/**
 * Parses theme JSON definitions into [NeonThemeConfig] using `org.json.JSONObject`
 * so theme assets can be validated identically on Android and in JVM unit tests.
 */
object ThemeJsonParser {
    const val DEFAULT_THEME_ID = "cyber-cyan"

    val CyberCyanFallback: NeonThemeConfig = NeonThemeConfig(
        id = DEFAULT_THEME_ID,
        name = "Cyber Cyan",
        nameEs = "Cian Cyber",
        author = "Neon Launcher",
        backgroundStyle = BackgroundStyle.AURORA,
        colors = ThemeColors(
            backgroundStart = 0xFF05060FL,
            backgroundEnd = 0xFF0B0F24L,
            surface = 0x14FFFFFFL,
            surfaceFocused = 0x26FFFFFFL,
            surfaceBorder = 0x4000F0FFL,
            accentPrimary = 0xFF00F0FFL,
            accentSecondary = 0xFFFF2BD6L,
            accentTertiary = 0xFF8A5CFFL,
            textPrimary = 0xFFF4F7FFL,
            textSecondary = 0xFFA8B3CFL,
            danger = 0xFFFF4D6DL,
        ),
    )

    /**
     * Parses [jsonText] into a [NeonThemeConfig], throwing [IllegalArgumentException]
     * if required fields or color values are invalid.
     */
    fun parse(jsonText: String): NeonThemeConfig {
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid theme JSON", e)
        }

        val id = requireNonBlank(root, "id")
        val name = requireNonBlank(root, "name")
        val nameEs = optNonBlank(root, "nameEs", "name_es")
        val author = optNonBlank(root, "author") ?: "Neon Launcher"
        val styleRaw = requireNonBlank(root, "backgroundStyle", "background_style")
        val backgroundStyle = parseBackgroundStyle(styleRaw)

        val colorsObj = root.optJSONObject("colors")
            ?: throw IllegalArgumentException("Missing 'colors' object in theme '$id'")

        val colors = ThemeColors(
            backgroundStart = requireColor(colorsObj, "backgroundStart", "background_start"),
            backgroundEnd = requireColor(colorsObj, "backgroundEnd", "background_end"),
            surface = requireColor(colorsObj, "surface"),
            surfaceFocused = requireColor(colorsObj, "surfaceFocused", "surface_focused"),
            surfaceBorder = requireColor(colorsObj, "surfaceBorder", "surface_border"),
            accentPrimary = requireColor(colorsObj, "accentPrimary", "accent_primary"),
            accentSecondary = requireColor(colorsObj, "accentSecondary", "accent_secondary"),
            accentTertiary = requireColor(colorsObj, "accentTertiary", "accent_tertiary"),
            textPrimary = requireColor(colorsObj, "textPrimary", "text_primary"),
            textSecondary = requireColor(colorsObj, "textSecondary", "text_secondary"),
            danger = requireColor(colorsObj, "danger"),
        )

        return NeonThemeConfig(
            id = id,
            name = name,
            nameEs = nameEs,
            author = author,
            backgroundStyle = backgroundStyle,
            colors = colors,
        )
    }

    /** Parses [jsonText], returning `null` instead of throwing if invalid. */
    fun parseOrNull(jsonText: String): NeonThemeConfig? = runCatching { parse(jsonText) }.getOrNull()

    /** Parses `#RRGGBB`, `#AARRGGBB`, `0xRRGGBB`, or `0xAARRGGBB` into an ARGB [Long]. */
    fun parseColorHex(raw: String): Long {
        val trimmed = raw.trim()
        val hex = when {
            trimmed.startsWith("#") -> trimmed.substring(1)
            trimmed.startsWith("0x", ignoreCase = true) -> trimmed.substring(2)
            else -> throw IllegalArgumentException("Color must start with '#' or '0x': '$raw'")
        }
        return when (hex.length) {
            6 -> 0xFF000000L or hex.toLong(16)
            8 -> hex.toLong(16) and 0xFFFFFFFFL
            else -> throw IllegalArgumentException("Color must have 6 or 8 hex digits: '$raw'")
        }
    }

    fun parseBackgroundStyle(raw: String): BackgroundStyle {
        val normalized = raw.trim().uppercase()
        return BackgroundStyle.entries.firstOrNull { it.name == normalized }
            ?: throw IllegalArgumentException("Unknown backgroundStyle '$raw'")
    }

    private fun requireColor(obj: JSONObject, vararg keys: String): Long {
        val raw = requireNonBlank(obj, *keys)
        return parseColorHex(raw)
    }

    private fun requireNonBlank(obj: JSONObject, vararg keys: String): String {
        return optNonBlank(obj, *keys)
            ?: throw IllegalArgumentException("Missing required field '${keys.first()}'")
    }

    private fun optNonBlank(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.optString(key, "").trim()
            if (value.isNotEmpty()) return value
        }
        return null
    }
}
