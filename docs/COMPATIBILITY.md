# Hardware & OS Compatibility

> [!IMPORTANT]
> **Verified vs. Unverified Status Notice**
>
> - **Verified locally**: Build & static checks (`./gradlew lint test assembleDebug assembleRelease` — 0 lint errors, 11/11 unit tests passing), universal APK packaging across `arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64`, zero GMS/Firebase/`INTERNET`/`QUERY_ALL_PACKAGES` audit, and `--dry-run` script checks.
> - **Pending verification**: **Runtime behavior on physical TV hardware, vendor firmwares, and Android TV emulators is currently UNVERIFIED and awaiting real device or emulator test reports** (no Android TV emulator image was available in the build environment). No compatibility rows below will be populated until real results are observed and verified.

---

## Target Platforms

| Platform | Expected Support | Notes |
| -------- | ---------------- | ----- |
| **Android TV** (`minSdk 23` / Android 6.0+) | Primary target | Uses `LEANBACK_LAUNCHER` banners and D-pad focus navigation. Default HOME switching method varies by OEM firmware. |
| **Google TV** (Android 10+ / API 29+) | Primary target | Google TV launcher (`com.google.android.apps.tv.launcherx` in common setups) often requires non-destructive ADB (`cmd role`) or Level 3 disable depending on firmware locks. |
| **AOSP TV Boxes** (with `android.software.leanback`) | Supported target | Many AOSP boxes expose the standard system HOME picker (`Always` / `Just once`) directly. |
| **Amazon Fire TV** (Fire OS) | Partial / Best-effort | Fire OS is Android-based and can run APKs sideloaded manually, but newer Fire OS releases restrict `HOME` role overrides and `pm disable-user` on `com.amazon.tv.launcher`. Needs device verification. |

### Why `minSdk 23` (Android 6.0 Marshmallow)?

`minSdk 23` is the **lowest API level compatible with the 2026 Jetpack Compose and AndroidX library baseline** used by Neon Launcher (`compileSdk 37` / `targetSdk 37`), ensuring modern TV Material / Compose focus reliability without abandoning older Android TV hardware.

---

## ADB Default-Home Command Matrix by Android Version

The table below documents standard AOSP ADB mechanisms by API level. **Whether a given TV firmware honors or restricts each command requires real device verification.**

| Android Version | API Level | Primary Non-Destructive Commands | Last-Resort Fallback | Verification Status |
| --------------- | --------- | -------------------------------- | -------------------- | ------------------- |
| **Android 6.0 – 9** | API 23 – 28 | System HOME picker (`HOME` key); `cmd package set-home-activity dev.neonlauncher/.HomeAlias` (API 24+ / firmware-dependent) | `pm disable-user --user 0 <stock-pkg>` | **Requires device verification** |
| **Android 10 – 11** | API 29 – 30 | `RoleManager` (`ROLE_HOME`); `cmd package set-home-activity dev.neonlauncher/.HomeAlias`; `cmd role add-role-holder android.app.role.HOME dev.neonlauncher` | `pm disable-user --user 0 <stock-pkg>` | **Requires device verification** |
| **Android 12+** | API 31 – 37 | `RoleManager` (`ROLE_HOME`); `cmd package set-home-activity dev.neonlauncher/.HomeAlias`; `cmd role add-role-holder android.app.role.HOME dev.neonlauncher` | `pm disable-user --user 0 <stock-pkg>` (blocked on some locked OEM/Fire OS builds) | **Requires device verification** |

---

## Verified Device Compatibility Matrix

### Legend

- `✅ Default Home` — Installs cleanly and launches reliably when pressing the remote **HOME** button.
- `⚠️ Manual Only` — Installs and runs properly when opened manually, but the remote **HOME** button returns to the vendor launcher (or requires a third-party button mapper).
- `❌ Unsupported` — Fails to install, crashes on launch, or incompatible with the device OS.

### Community-Verified Devices

| Brand | Model | Android / API | Status | Method | Notes |
| ----- | ----- | ------------- | ------ | ------ | ----- |
| *(None yet)* | *Pending community testing* | — | — | — | Run `scripts/diagnose` and submit a **Hardware Compatibility Report** issue to add the first verified entry. |

---

## How to Submit a Compatibility Report

1. Install and test Neon Launcher on your TV using [`docs/INSTALL.md`](INSTALL.md).
2. Run the sanitized diagnostic script from your computer (it **never** outputs serial numbers, MAC addresses, IP addresses, or user accounts):

   ```bash
   ./scripts/diagnose.sh <TV_IP>
   ```

   Or in Windows PowerShell:

   ```powershell
   .\scripts\diagnose.ps1 <TV_IP>
   ```

3. Open a new **Hardware Compatibility Report** issue on GitHub (`https://github.com/OWNER/REPO/issues/new/choose`) and paste the generated Markdown block along with which HOME setup method worked (and whether `Return to Stock Launcher` worked cleanly).
