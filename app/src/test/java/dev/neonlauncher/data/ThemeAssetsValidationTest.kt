package dev.neonlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeAssetsValidationTest {
    @Test
    fun bundledThemeAssetsAreValidAndComplete() {
        val themesDir = resolveThemesDir()
        assertTrue("Expected themes directory at ${themesDir.absolutePath}", themesDir.isDirectory)

        val jsonFiles = themesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        }?.sortedBy { it.name }.orEmpty()

        assertTrue(
            "Expected at least 4 bundled themes, found ${jsonFiles.size}",
            jsonFiles.size >= 4,
        )

        val seenIds = mutableSetOf<String>()
        for (file in jsonFiles) {
            val config = ThemeJsonParser.parse(file.readText(Charsets.UTF_8))
            assertTrue("Theme id must be non-blank", config.id.isNotBlank())
            assertTrue("Theme name must be non-blank", config.name.isNotBlank())
            assertTrue("Theme author must be non-blank", config.author.isNotBlank())
            assertEquals(
                "Theme id '${config.id}' must match filename '${file.name}'",
                "${config.id}.json",
                file.name,
            )
            assertTrue("Duplicate theme id '${config.id}'", seenIds.add(config.id))
            assertTrue(config.backgroundStyle in BackgroundStyle.entries)
            assertValidArgbColor(config.colors.backgroundStart)
            assertValidArgbColor(config.colors.backgroundEnd)
            assertValidArgbColor(config.colors.surface)
            assertValidArgbColor(config.colors.surfaceFocused)
            assertValidArgbColor(config.colors.surfaceBorder)
            assertValidArgbColor(config.colors.accentPrimary)
            assertValidArgbColor(config.colors.accentSecondary)
            assertValidArgbColor(config.colors.accentTertiary)
            assertValidArgbColor(config.colors.textPrimary)
            assertValidArgbColor(config.colors.textSecondary)
            assertValidArgbColor(config.colors.danger)
        }

        assertTrue(
            "Default theme '${ThemeJsonParser.DEFAULT_THEME_ID}' must exist in assets/themes",
            ThemeJsonParser.DEFAULT_THEME_ID in seenIds,
        )
    }

    @Test
    fun parseColorHexSupportsRgbAndArgbAndRejectsMalformed() {
        assertEquals(0xFF00F0FFL, ThemeJsonParser.parseColorHex("#00F0FF"))
        assertEquals(0x4000F0FFL, ThemeJsonParser.parseColorHex("#4000F0FF"))
        assertEquals(0xFF123456L, ThemeJsonParser.parseColorHex("0x123456"))

        val rejected = runCatching { ThemeJsonParser.parseColorHex("not-a-color") }
        assertFalse(rejected.isSuccess)
    }

    private fun assertValidArgbColor(value: Long) {
        assertTrue(
            "Color 0x${value.toString(16)} out of 32-bit ARGB range",
            value in 0L..0xFFFFFFFFL,
        )
    }

    private fun resolveThemesDir(): File {
        val candidates = listOf(
            File("src/main/assets/themes"),
            File("app/src/main/assets/themes"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/assets/themes from cwd=${File(".").absolutePath}")
    }
}
