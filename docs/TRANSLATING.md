# Translating Neon Launcher

Thank you for helping make **Neon Launcher** accessible in more languages!

Neon Launcher ships with **English** (default) and **Spanish** (`values-es`) out of the box, with all documentation maintained in English and Spanish (`README.md` and `README.es.md`).

---

## 1. Translating Android UI Strings

### File Locations

- Default English source: `app/src/main/res/values/strings.xml`
- Existing Spanish translation: `app/src/main/res/values-es/strings.xml`
- Your new language: `app/src/main/res/values-<bcp47>/strings.xml`

Common Android resource folder qualifiers:

| Language | Folder |
| -------- | ------ |
| French | `app/src/main/res/values-fr/strings.xml` |
| German | `app/src/main/res/values-de/strings.xml` |
| Portuguese (Brazil) | `app/src/main/res/values-pt-rBR/strings.xml` |
| Italian | `app/src/main/res/values-it/strings.xml` |
| Japanese | `app/src/main/res/values-ja/strings.xml` |
| Arabic (RTL) | `app/src/main/res/values-ar/strings.xml` |

### Rules for `strings.xml`

1. **Preserve placeholders exactly**: Keep format specifiers such as `%1$d` and `%1$s` untouched (for example, `<string name="home_apps_count">%1$d apps</string>` or `<string name="onboarding_compat_android">Android %1$s (API %2$d)</string>`).
2. **Escape XML special characters**:
   - Apostrophe: `\'` (e.g., `Neon Launcher\'s`)
   - Ampersand: `&amp;` (e.g., `Display &amp; Sound`)
   - Less-than / greater-than: `&lt;` / `&gt;`
3. **Keep TV labels concise**: Android TV cards, buttons, and hint bars are viewed from across a living room—avoid overly long labels that wrap awkwardly or truncate on 1080p/4K TV overscan margins.
4. **Keep D-pad terminology consistent**: Refer cleanly to `OK`, `Hold OK`, `Menu`, `Back`, and `D-Pad` in ways familiar to TV remote users in your language.

---

## 2. Right-to-Left (RTL) Guidelines

If adding an RTL language (such as Arabic `ar`, Hebrew `iw`/`he`, or Persian `fa`):

- Verify that Android TV layouts look mirrored correctly (using `Start`/`End` alignment rather than hardcoded `Left`/`Right`).
- Note in your pull request if any D-pad directional hints or row alignments need RTL adjustments.
- Avoid embedding directional arrow glyphs (`→`, `←`) inside strings unless appropriate for RTL reading order.

---

## 3. Translating Documentation (Optional)

You can also translate `README.md` into `README.<lang>.md` (following the layout of [`README.es.md`](../README.es.md)) and link it in the language switcher at the top of `README.md`.

---

## 4. Verification & Pull Request Checklist

Before opening a pull request:

1. Run Android lint and unit tests to ensure no invalid XML escapes or missing format arguments exist:

   ```bash
   ./gradlew lint test
   ```

   *(If you don't have Android Studio installed locally, open a PR or test via the `Build` workflow on your GitHub fork.)*

2. Check your PR against this list:
   - [ ] All `<string name="...">` keys in `values/strings.xml` are present or intentionally inherited cleanly.
   - [ ] All format specifiers (`%1$s`, `%1$d`, `%2$d`) match the English source.
   - [ ] XML entities (`&amp;`, `\'`) are properly escaped.
   - [ ] `./gradlew lint test` passes without translation-related lint errors.
   - [ ] RTL behavior noted (if applicable).
