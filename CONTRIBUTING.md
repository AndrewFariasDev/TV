# Contributing to Neon Launcher

Thank you for helping improve **Neon Launcher** (`dev.neonlauncher`)! Contributions of all kinds are welcome: bug fixes, theme palettes, translations, hardware compatibility reports, documentation updates, and script improvements.

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

---

## Ways to Contribute

1. **Test on Real TV Hardware**: Run [`scripts/diagnose.sh`](scripts/diagnose.sh) or [`scripts/diagnose.ps1`](scripts/diagnose.ps1) and submit a **Compatibility Report** issue with the sanitized Markdown output.
2. **Add a Built-in Theme**: Follow [`docs/THEMING.md`](docs/THEMING.md) to add a new JSON theme under `app/src/main/assets/themes/` and validate it with `./gradlew test`.
3. **Add or Improve Translations**: Follow [`docs/TRANSLATING.md`](docs/TRANSLATING.md) to add or refine `app/src/main/res/values-<lang>/strings.xml` or documentation.
4. **Fix Bugs or Improve D-pad UX**: Open a pull request addressing an open issue or usability gap.

---

## Development Prerequisites

- **JDK 17** (Temurin 17 is used in CI)
- **Android SDK** targeting `compileSdk 37` / `targetSdk 37` (`minSdk 23`)
- An Android TV / Google TV device or Android TV emulator for D-pad testing

No Android Studio installation on your machine? You can still build debug APKs directly on GitHub via your fork's **Actions** tab (see the instructions in [`README.md`](README.md)).

---

## Local Build & Verification

Run lint, unit tests (including theme JSON schema validation), and build the debug APK:

```bash
chmod +x gradlew
./gradlew lint test assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat lint test assembleDebug
```

---

## Project Standards

All pull requests must adhere to these core rules:

1. **D-Pad First Navigation**
   - Every interactive UI element must be cleanly reachable via D-pad directional keys (`Up`/`Down`/`Left`/`Right`).
   - `OK` / `DPAD_CENTER` launches or activates the focused item.
   - `Hold OK` (long press) or the remote `Menu` button opens app options.
   - `Back` must **never** exit the launcher or strand the user outside `HomeAlias`.
2. **Strict Offline Privacy**
   - **Never** add `android.permission.INTERNET`, `android.permission.QUERY_ALL_PACKAGES`, GMS, Firebase, analytics, telemetry, or ad libraries.
3. **Bilingual Baseline (EN + ES)**
   - Any PR adding or modifying user-facing Android strings in `app/src/main/res/values/strings.xml` must also update `app/src/main/res/values-es/strings.xml` (see [`docs/TRANSLATING.md`](docs/TRANSLATING.md)).
4. **Validated Theme JSONs**
   - New or modified JSON themes must follow the schema in [`docs/THEMING.md`](docs/THEMING.md) and pass `./gradlew test`.
5. **Non-Destructive Scripts & Sanitized Output**
   - Any changes under `scripts/` must preserve `--dry-run` / `-DryRun`, print rollback commands before acting, require explicit confirmation, and never log personal IP addresses, device serial numbers, MAC addresses, or user accounts.
   - Never commit personal IPs, serial numbers, keystores, or maintainer-specific handles (`OWNER/REPO` placeholders only in docs/templates).

---

## Pull Request Checklist

Before opening a pull request, ensure:

- [ ] `./gradlew lint test assembleDebug` passes locally (or in your fork's `Build` workflow).
- [ ] D-pad controls (`OK`, `Hold OK` / `Menu`, `Back`) are verified where applicable.
- [ ] English and Spanish strings are updated together.
- [ ] Theme JSON files (if changed) are validated by unit tests.
- [ ] No `INTERNET`, `QUERY_ALL_PACKAGES`, GMS, or telemetry additions exist.
- [ ] Documentation (`README.md`, `README.es.md`, `docs/*.md`, `CHANGELOG.md`) is updated if behavior changed.
