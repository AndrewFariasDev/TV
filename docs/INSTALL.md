# Installation & Default Launcher Guide

This guide walks you through installing **Neon Launcher** (`dev.neonlauncher`) on Android TV, Google TV, and compatible TV boxes—from the easiest no-PC method to safe ADB setup and manual recovery.

> [!NOTE]
> Minimum supported Android version: **Android 6.0 Marshmallow (`minSdk 23`)**, targeting **API 37**. Hardware compatibility varies by TV manufacturer—see [`COMPATIBILITY.md`](COMPATIBILITY.md).

---

## Step 1: Install Neon Launcher

Choose whichever installation method is easiest for you.

### Option A: TV "Downloader" App (No PC Required)

1. On your TV, install the **Downloader** app (by AFTVnews) from your TV's app store.
2. Enable **"Install unknown apps"** permission for Downloader:
   - Open TV **Settings** → **Apps** (or **Security & restrictions**) → **Install unknown apps** → enable **Downloader**.
3. Open **Downloader** and enter the direct release URL (or a trusted URL shortener pointing directly to this URL):

   ```text
   https://github.com/OWNER/REPO/releases/latest/download/neon-launcher.apk
   ```

4. When the download finishes, select **Install** and confirm.
5. Open **Neon Launcher** from your Apps row to test it safely before changing your default HOME screen.

---

### Option B: From a PC via ADB (`scripts/install`)

Use the repository's non-destructive install script, which automatically verifies the SHA-256 checksum of `neon-launcher.apk` and prints the uninstall rollback command before changing anything.

1. On your TV, enable Developer Options:
   - Open TV **Settings** → **Device Preferences** (or **System**) → **About** → press **OK** 7 times on **Build** until Developer Options are enabled.
2. Enable **USB debugging** (for a USB cable) or **Network debugging** / **Wireless debugging** (for Wi-Fi).
3. Update `GITHUB_REPO` in [`scripts/config.env`](../scripts/config.env) if installing from your own fork, or pass `--apk` / `-Apk` for a local file.
4. Preview first with `--dry-run` / `-DryRun`:

   **Linux / macOS (Bash):**
   ```bash
   ./scripts/install.sh <TV_IP> --dry-run
   ./scripts/install.sh <TV_IP>
   ```

   **Windows (PowerShell):**
   ```powershell
   .\scripts\install.ps1 <TV_IP> -DryRun
   .\scripts\install.ps1 <TV_IP>
   ```

   *(If Windows reports that running scripts is disabled, run `powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1 <TV_IP> -DryRun`, or enable local scripts once with `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`. If using USB ADB instead of Wi-Fi, omit `<TV_IP>` or pass `--serial <id>` / `-Serial <id>`.)*

---

### Option C: USB Flash Drive + TV File Explorer

1. On a computer, download both files from `https://github.com/OWNER/REPO/releases/latest`:
   - `neon-launcher.apk`
   - `neon-launcher.apk.sha256`
2. Verify the SHA-256 checksum before copying:

   ```bash
   sha256sum -c neon-launcher.apk.sha256
   ```

   Or in Windows PowerShell:

   ```powershell
   Get-FileHash .\neon-launcher.apk -Algorithm SHA256
   Get-Content .\neon-launcher.apk.sha256
   ```

3. Copy `neon-launcher.apk` to a FAT32 or exFAT USB drive.
4. Plug the USB drive into your TV, open a TV file manager app, grant **"Install unknown apps"** when prompted, and select `neon-launcher.apk`.

---

## Step 2: Set as Default HOME Launcher (Least to Most Invasive)

Always start with **Level 1** and only move down if your TV manufacturer blocks earlier methods.

### Level 1 (Recommended): In-App Setup / System HOME Chooser / `RoleManager`

1. Launch **Neon Launcher** normally from your Apps list.
2. In the **Welcome Guide** (or **Neon Settings** → **Set as Default Home**), select **Set as Home**.
3. On Android 10+ (API 29+) devices that expose `RoleManager`, Android presents a system **Home app** picker where you can select **Neon Launcher**.
4. On Android 6–9 (API 23–28) devices, pressing the physical **HOME** button after enabling `HomeAlias` may open Android's **"Complete action using"** prompt—choose **Neon Launcher** → **Always**.

> [!NOTE]
> Many Android TV / Google TV manufacturers hide the system Home picker dialog. If Level 1 does not switch the HOME button, proceed to Level 2.

---

### Level 2 (Non-Destructive ADB): `scripts/set-default-launcher`

This method keeps your factory launcher enabled and intact while asking Android to route `HOME` to `dev.neonlauncher/.HomeAlias`:

**Linux / macOS (Bash):**
```bash
./scripts/set-default-launcher.sh <TV_IP> --dry-run
./scripts/set-default-launcher.sh <TV_IP>
```

**Windows (PowerShell):**
```powershell
.\scripts\set-default-launcher.ps1 <TV_IP> -DryRun
.\scripts\set-default-launcher.ps1 <TV_IP>
```

Under the hood, this runs two non-destructive commands:

1. `adb shell cmd package set-home-activity dev.neonlauncher/.HomeAlias`
2. On Android 10+ (API 29+): `adb shell cmd role add-role-holder android.app.role.HOME dev.neonlauncher`

---

### Level 3 (Last Resort Only): `pm disable-user` of the Stock Launcher

Some OEM TVs aggressively reset `set-home-activity` or block `cmd role` and keep booting back into the vendor launcher. As a **last resort**, disabling the vendor launcher forces Android to fall back to `dev.neonlauncher/.HomeAlias`.

> [!WARNING]
> **Understand the risk before proceeding:** If you disable your stock launcher and later uninstall Neon Launcher (or trigger Safe Mode) without re-enabling the stock launcher first, pressing **HOME** will have no fallback until you re-enable the stock launcher via ADB.

First, run [`scripts/diagnose.sh`](../scripts/diagnose.sh) or [`scripts/diagnose.ps1`](../scripts/diagnose.ps1) to confirm the exact stock launcher package on **your** device. Common examples include (your device may differ):

- `com.google.android.tvlauncher` (classic Android TV)
- `com.google.android.apps.tv.launcherx` (Google TV)
- `com.amazon.tv.launcher` (Fire TV — note: newer Fire OS builds often block `pm disable-user` without elevated permissions)

You can use `--disable-stock` / `-DisableStock` in the helper script, which prints exact rollback commands and asks for **double confirmation**:

```bash
./scripts/set-default-launcher.sh <TV_IP> --disable-stock --dry-run
./scripts/set-default-launcher.sh <TV_IP> --disable-stock
```

```powershell
.\scripts\set-default-launcher.ps1 <TV_IP> -DisableStock -DryRun
.\scripts\set-default-launcher.ps1 <TV_IP> -DisableStock
```

**Exact manual reversal command** (if you ever need to re-enable a disabled stock launcher):

```bash
adb shell pm enable <stock-launcher-package>
```

---

## Step 3: Manual Recovery & Returning to Stock

### From the TV Remote (No PC Needed)

1. **In-App Exit**: Open **Neon Settings** → **Return to Stock Launcher** → confirm **Return to Stock**. This disables `dev.neonlauncher/.HomeAlias` and hands `HOME` back to the system.
2. **TV System Settings**: Open TV **Settings** → **Apps** → **See all apps** → **Neon Launcher** → **Uninstall** (or **Default apps** → **Home app**, if your TV exposes it).

> [!CAUTION]
> If you used Level 3 (`pm disable-user`) to disable your stock launcher, **re-enable the stock launcher via ADB before uninstalling Neon Launcher**, or use `scripts/restore-stock-launcher` which always re-enables the stock launcher first.

### From a PC via ADB

Use the restore script (always preview with `--dry-run` / `-DryRun` first):

```bash
./scripts/restore-stock-launcher.sh <TV_IP> --dry-run
./scripts/restore-stock-launcher.sh <TV_IP>
```

```powershell
.\scripts\restore-stock-launcher.ps1 <TV_IP> -DryRun
.\scripts\restore-stock-launcher.ps1 <TV_IP>
```

Or run the raw ADB commands manually (replace `<stock-package>` and `<stock-component>` with your device's values from `diagnose`):

```bash
adb shell pm enable <stock-package>
adb shell pm disable-user --user 0 dev.neonlauncher/.HomeAlias
adb shell cmd package set-home-activity <stock-component>
# On Android 10+ (API 29+):
adb shell cmd role add-role-holder android.app.role.HOME <stock-package>
```
