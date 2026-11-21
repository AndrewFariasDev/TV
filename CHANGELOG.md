# Changelog

All notable changes to **Neon Launcher** (`dev.neonlauncher`) will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-21

### Added

- **Android TV Home Launcher (`dev.neonlauncher`)**:
  - `minSdk 23` (Android 6.0 Marshmallow, lowest compatible with the 2026 Jetpack Compose / AndroidX baseline) targeting `targetSdk 37` / `compileSdk 37`.
  - Dedicated `dev.neonlauncher/.HomeAlias` HOME activity and `dev.neonlauncher/.MainActivity` launcher entry point.
  - D-pad first navigation: `OK` to launch, `Hold OK` or `Menu` for app options context menu, and non-exiting `Back` handling.
- **Anti-Trap Safety Safeguards**:
  - **Return to Stock Launcher** option in Settings to switch back safely from the remote.
  - **3-Crash-in-2-Minute Safe Mode** automatic crash-loop recovery protection.
- **Strict Offline Privacy Model**:
  - Zero `INTERNET` permission, zero `QUERY_ALL_PACKAGES`, zero GMS/Firebase, zero telemetry, zero ads.
  - Explicit `<queries>` app/settings discovery and system-confirmed `REQUEST_DELETE_PACKAGES` uninstall support.
- **Theme Engine & Localization**:
  - JSON-based themes (`AURORA`, `GRID`, `SOLID` background styles) under `app/src/main/assets/themes/`.
  - Bilingual English and Spanish UI and documentation.
- **CI/CD & Open-Source Tooling**:
  - GitHub Actions `Build` (`lint test assembleDebug`) and `Release` (`neon-launcher.apk` + `neon-launcher.apk.sha256`, optional keystore signing with `debug-fallback` notice).
  - Non-destructive Bash and PowerShell helper scripts (`install`, `diagnose`, `set-default-launcher`, `restore-stock-launcher`, `generate-keystore`) with `--dry-run` / `-DryRun` and rollback-first prompts.
  - Complete documentation in `docs/` (`INSTALL.md`, `COMPATIBILITY.md`, `TROUBLESHOOTING.md`, `THEMING.md`, `TRANSLATING.md`).

### Notes

- Hardware runtime compatibility across Android TV, Google TV, AOSP TV boxes, and Fire TV devices is currently **unverified** and pending community testing reports in [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md).
