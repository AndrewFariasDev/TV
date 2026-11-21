# Creating Custom Themes for Neon Launcher

Neon Launcher loads bundled themes from JSON files stored in:

```text
app/src/main/assets/themes/*.json
```

All `.json` files in that directory are discovered automatically at runtime by `ThemeRepository` and validated by `ThemeJsonParser` (and unit tests via `./gradlew test`).

---

## Built-in Themes

| ID | English Name | Spanish Name (`nameEs`) | Default Style | File |
| -- | ------------ | ----------------------- | ------------- | ---- |
| `cyber-cyan` *(default)* | Cyber Cyan | Cian Cyber | `AURORA` | `app/src/main/assets/themes/cyber-cyan.json` |
| `emerald-matrix` | Emerald Matrix | Matriz Esmeralda | `GRID` | `app/src/main/assets/themes/emerald-matrix.json` |
| `synthwave-grid` | Synthwave Grid | Rejilla Synthwave | `GRID` | `app/src/main/assets/themes/synthwave-grid.json` |
| `ultraviolet-void` | Ultraviolet Void | Vacío Ultravioleta | `AURORA` | `app/src/main/assets/themes/ultraviolet-void.json` |

---

## Theme JSON Schema

Every theme file must be a valid UTF-8 JSON object matching `ThemeJsonParser`:

```json
{
  "id": "cyber-cyan",
  "name": "Cyber Cyan",
  "nameEs": "Cian Cyber",
  "author": "Neon Launcher",
  "backgroundStyle": "AURORA",
  "colors": {
    "backgroundStart": "#FF05060F",
    "backgroundEnd": "#FF0B0F24",
    "surface": "#14FFFFFF",
    "surfaceFocused": "#26FFFFFF",
    "surfaceBorder": "#4000F0FF",
    "accentPrimary": "#FF00F0FF",
    "accentSecondary": "#FFFF2BD6",
    "accentTertiary": "#FF8A5CFF",
    "textPrimary": "#FFF4F7FF",
    "textSecondary": "#FFA8B3CF",
    "danger": "#FFFF4D6D"
  }
}
```

### Top-Level Fields

| Field | Required | Type | Description |
| ----- | -------- | ---- | ----------- |
| `id` | Yes | String | Unique lowercase kebab-case identifier (should match filename `<id>.json`). |
| `name` | Yes | String | Display name shown in English UI. |
| `nameEs` | Recommended | String | Display name shown when TV language is Spanish (falls back to `name` if omitted). |
| `author` | Optional | String | Creator attribution shown/stored in metadata (defaults to `"Neon Launcher"` if omitted). |
| `backgroundStyle` | Yes | Enum | One of `"AURORA"`, `"GRID"`, or `"SOLID"`. Users can still override background style in Settings. |
| `colors` | Yes | Object | Must contain all **11** required color keys below. |

### `colors` Fields (All 11 Required)

Color values must be 6-digit `#RRGGBB` or 8-digit `#AARRGGBB` hex strings (with alpha first in `#AARRGGBB`).

| Field | Format | Role in UI |
| ----- | ------ | ---------- |
| `backgroundStart` | `#AARRGGBB` / `#RRGGBB` | Top/primary backdrop gradient color. |
| `backgroundEnd` | `#AARRGGBB` / `#RRGGBB` | Bottom/secondary backdrop gradient color. |
| `surface` | `#AARRGGBB` / `#RRGGBB` | Unfocused card, tile, and panel fill (translucent `#14FFFFFF` works well over dark backdrops). |
| `surfaceFocused` | `#AARRGGBB` / `#RRGGBB` | Highlighted fill when a card or button receives D-pad focus. |
| `surfaceBorder` | `#AARRGGBB` / `#RRGGBB` | Subtle border/stroke around cards and containers. |
| `accentPrimary` | `#AARRGGBB` / `#RRGGBB` | Primary focus glow/ring, active badges, and primary controls. |
| `accentSecondary` | `#AARRGGBB` / `#RRGGBB` | Secondary aurora bloom, gradients, and highlights. |
| `accentTertiary` | `#AARRGGBB` / `#RRGGBB` | Tertiary accent for subtle depth and ambient effects. |
| `textPrimary` | `#AARRGGBB` / `#RRGGBB` | Main titles, app labels, clock, and primary body text. |
| `textSecondary` | `#AARRGGBB` / `#RRGGBB` | Subtitles, hints, metadata counts, and helper descriptions. |
| `danger` | `#AARRGGBB` / `#RRGGBB` | Destructive actions (Uninstall, Return to Stock Launcher, Safe Mode warnings). |

---

## TV Accessibility & WCAG Contrast Tips

Living-room TVs vary widely in panel brightness, gamma, and viewing distance:

1. **Aim for high text contrast (WCAG AA / AAA)**:
   - Keep `textPrimary` vs. `backgroundStart`/`backgroundEnd` (and `textPrimary` over `surface` / `surfaceFocused`) at **≥ 7:1** where possible (at least **4.5:1** minimum).
   - Keep `textSecondary` vs. background/surface at **≥ 4.5:1** so hint bars and subtitles remain readable from a couch.
2. **Keep backdrops dark (`backgroundStart` / `backgroundEnd`)**:
   - Neon accents glow cleanly over deep low-luminance backgrounds without washing out TV panels or causing glare in dark rooms.
3. **Make focus unmistakable**:
   - Ensure `accentPrimary` and `surfaceFocused` contrast sharply against `surface` and `backgroundStart`/`backgroundEnd` so users always see which tile has D-pad focus (important for **High Contrast Mode** and **Performance Mode**, where glow/blur is reduced).
4. **Verify `danger` legibility**:
   - Destructive buttons and Safe Mode banners must remain immediately distinguishable from `accentPrimary`.

---

## Validating & Contributing a Theme

1. Save your theme as `app/src/main/assets/themes/<your-theme-id>.json`.
2. Run Gradle tests and lint to validate JSON parsing and color formats:

   ```bash
   ./gradlew test lint
   ```

   On Windows PowerShell:

   ```powershell
   .\gradlew.bat test lint
   ```

   *(No Android Studio locally? Fork the repository, commit your theme JSON, enable Actions on your fork, and run the **`Build`** workflow!)*

3. Open a Pull Request following [`.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md) and [`CONTRIBUTING.md`](../CONTRIBUTING.md):
   - Include screenshots or color contrast notes if possible.
   - Confirm `./gradlew test` passes cleanly.
   - Include both `name` and `nameEs` for bilingual support.
