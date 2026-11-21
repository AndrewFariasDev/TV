# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| `1.0.x` | Yes       |
| `< 1.0` | No        |

## Reporting a Vulnerability

**Please do NOT open public GitHub issues for security vulnerabilities or crash-loop exploits.**

Instead, report vulnerabilities privately via GitHub Security Advisories:

- `https://github.com/OWNER/REPO/security/advisories/new`

When submitting a report, please include:

- A clear description of the vulnerability and potential impact
- Steps to reproduce (without including personal serial numbers, IP addresses, MAC addresses, or Google accounts)
- Target Android TV / Google TV version, API level, and Neon Launcher version

Maintainers aim to acknowledge private reports within **72 hours** and provide an assessment or remediation plan as quickly as possible.

---

## Architecture & Privacy Guarantees

Neon Launcher (`dev.neonlauncher`) is designed around strict least-privilege and offline operation:

- **Zero network access**: `android.permission.INTERNET` is **not** requested anywhere in the app manifest. The launcher cannot phone home, fetch ads, or exfiltrate telemetry.
- **Zero broad package snooping**: `android.permission.QUERY_ALL_PACKAGES` is **not** requested. App discovery uses explicit Android `<queries>` declarations for launchable apps (`MAIN`/`LAUNCHER`, `LEANBACK_LAUNCHER`) and system settings intents.
- **Zero proprietary tracking SDKs**: No Google Mobile Services (GMS), Firebase, analytics, or ad SDKs are bundled.
- **User-confirmed uninstall only**: `android.permission.REQUEST_DELETE_PACKAGES` triggers the standard Android system confirmation dialog; the launcher cannot silently uninstall apps.

---

## Anti-Trap Safety Guarantees

Replacing a TV's default HOME screen carries a usability risk if an app crashes on boot or hides the exit path. Neon Launcher implements multiple safeguards so users are never trapped:

1. **Return to Stock Launcher (In-App Exit Path)**  
   A dedicated **Return to Stock Launcher** action inside Neon Launcher's Settings lets users disable `dev.neonlauncher/.HomeAlias` or open the system HOME chooser directly from the TV remote without needing a PC.

2. **3-Crash-in-2-Minute Safe Mode**  
   If Neon Launcher crashes **3 times within 2 minutes**, automatic crash-loop protection activates **Safe Mode** so users can recover settings, reset a problematic theme, or return to the stock launcher instead of being stuck in an infinite crash loop.

3. **Non-Destructive Setup & Rollback Scripts**  
   - All helper scripts in `scripts/` support `--dry-run` / `-DryRun`.
   - Every state-changing script prints exact rollback/restore commands **before** asking for confirmation.
   - `scripts/set-default-launcher` prefers non-destructive `cmd package set-home-activity` and `cmd role add-role-holder` over `pm disable-user`, requiring explicit `--disable-stock` + double confirmation for disabling the vendor launcher.
   - `scripts/diagnose` strictly omits device serial numbers, MAC addresses, IP addresses, and user accounts from its output.

---

## Verifying Release Downloads

Every GitHub Release attaches:

- `neon-launcher.apk`
- `neon-launcher.apk.sha256`

Verify integrity before installing:

```bash
sha256sum -c neon-launcher.apk.sha256
```

Or on Windows PowerShell:

```powershell
Get-FileHash .\neon-launcher.apk -Algorithm SHA256
Get-Content .\neon-launcher.apk.sha256
```

### Release Signing Note

- **Maintainer Release Keystore**: When repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) are configured, releases are signed with a consistent release key so updates install smoothly over prior releases.
- **Debug-Fallback Signing**: When those secrets are not configured (for example, on community forks), the release workflow builds with `SIGNING_MODE=debug-fallback` and documents this clearly in the release notes. Because Android enforces signature matching for updates, upgrading between incompatible signing keys requires uninstalling first (`adb uninstall dev.neonlauncher`).
