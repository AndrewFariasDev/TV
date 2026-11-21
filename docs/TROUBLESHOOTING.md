# Troubleshooting Guide

Common fixes for HOME button issues, Safe Mode recovery, missing apps, ADB connection errors, and APK signature update conflicts.

---

## 1. Stuck HOME Button / Cannot Exit or Cannot Enter Neon Launcher

### Case A: Pressing HOME Still Opens the Stock TV Launcher

- **Cause**: Some TV manufacturers ignore `RoleManager` or reset `cmd package set-home-activity` after reboots.
- **Fix**:
  1. Run [`scripts/diagnose.sh`](../scripts/diagnose.sh) (or `.ps1`) to see which HOME launchers are active and whether `cmd role` / `cmd package set-home-activity` are supported.
  2. Try Level 2 setup: `./scripts/set-default-launcher.sh <TV_IP>` (or `.ps1`).
  3. If your device firmware overrides Level 2, see Level 3 (`--disable-stock`) in [`docs/INSTALL.md`](INSTALL.md) and understand the rollback steps before proceeding.

### Case B: Stuck Inside Neon Launcher / Want to Return to Stock Launcher

- **With TV remote only**: Open **Neon Settings** → **Return to Stock Launcher** → confirm **Return to Stock**.
- **Via ADB (even if the UI won't open)**:
  Run the restore script, which re-enables any disabled stock launcher **before** disabling `dev.neonlauncher/.HomeAlias`:

  ```bash
  ./scripts/restore-stock-launcher.sh <TV_IP> --dry-run
  ./scripts/restore-stock-launcher.sh <TV_IP>
  ```

  ```powershell
  .\scripts\restore-stock-launcher.ps1 <TV_IP> -DryRun
  .\scripts\restore-stock-launcher.ps1 <TV_IP>
  ```

  Or manually:

  ```bash
  adb shell pm enable <stock-launcher-package>
  adb shell pm disable-user --user 0 dev.neonlauncher/.HomeAlias
  adb shell cmd package set-home-activity <stock-component>
  ```

---

## 2. Safe Mode Recovery (3 Crashes in 2 Minutes)

Neon Launcher includes built-in crash-loop protection: if the app crashes **3 times within 2 minutes**, it automatically enters **Safe Mode** and pauses its Home alias so your TV remains usable.

On the **Safe Mode Recovery** screen you have three options:

1. **Reset Settings** — Restores default theme, favorites, and hidden apps (helpful if a custom configuration or theme triggered an issue).
2. **Continue Manually** — Opens the launcher manually without re-enabling Home mode yet so you can inspect settings.
3. **Re-enable Home** — Reactivates `dev.neonlauncher/.HomeAlias` once the issue is resolved.

If you still cannot recover from the TV screen, run [`scripts/restore-stock-launcher`](../scripts/restore-stock-launcher.sh) via ADB and consider opening a Bug Report issue with the Diagnostic Summary shown on the Safe Mode screen (making sure not to include personal IPs, serials, or account info).

---

## 3. Missing Apps or Missing TV Banners

- **App hidden by user**: Open **Neon Settings** → **Hidden Apps**, focus the app, and press **OK** to restore it to Home.
- **Standard mobile/sideloaded apps**: Apps without an Android TV `LEANBACK_LAUNCHER` intent fall back to their standard `LAUNCHER` entry and standard icon rather than a wide TV banner.
- **System/background services**: Services without a launchable main activity will not appear in the app grid (by design, via `<queries>` for launchable activities).
- **Newly installed app not appearing immediately**: Wait a couple seconds for Android's package broadcast or reopen Neon Launcher.

---

## 4. ADB Connection Refused or `unauthorized`

### `Connection refused` / `cannot connect to <IP>:5555`

1. Verify both your PC and TV are on the **same local network / subnet** (disable client isolation or VPNs that block LAN traffic).
2. On the TV, check **Settings** → **Developer options** and ensure **USB debugging** / **Network debugging** (or **Wireless debugging**) is enabled.
3. Reboot the TV once after toggling debugging, as some TV boxes only bind port `5555` after a restart.
4. Some newer Android TV / Google TV builds use **Wireless debugging** with a random port instead of `5555`—pair first with `adb pair <IP>:<PAIR_PORT>` and then connect to the port shown on screen (`./scripts/install.sh <IP>:<PORT>`).

### `unauthorized` in `adb devices`

1. Look at your TV screen—an **"Allow USB debugging?"** RSA fingerprint dialog should be waiting for confirmation.
2. Check **"Always allow from this computer"** and select **Allow / OK**.
3. If the prompt did not appear, reset ADB on your computer and revoke authorizations on the TV:

   ```bash
   adb kill-server
   adb start-server
   ```

---

## 5. Debug-Signed APK Update Conflict (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`)

### Symptom

Installing a newer `neon-launcher.apk` fails with:

```text
adb: failed to install neon-launcher.apk: Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package dev.neonlauncher signatures do not match previously installed version; ignoring!]
```

### Cause

Android requires APK updates to carry the exact same signing certificate as the already-installed app. This error happens when:

- You previously installed a CI `debug-fallback` build (or a local debug build from another machine/fork) and are now installing a release-keystore build (or another debug build signed by a different machine).

### Fix

1. If you disabled your stock launcher (Level 3), **re-enable the stock launcher first** using `scripts/restore-stock-launcher` so you aren't left without a HOME app.
2. Uninstall the existing Neon Launcher package:

   ```bash
   adb uninstall dev.neonlauncher
   ```

3. Install the new APK cleanly:

   ```bash
   ./scripts/install.sh <TV_IP>
   ```
