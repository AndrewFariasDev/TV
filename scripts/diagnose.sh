#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "$SCRIPT_DIR/_common.sh"

usage() {
  cat <<'EOF'
Usage: diagnose.sh [TV_IP] [--serial SERIAL] [--dry-run] [-h|--help]

Collect non-sensitive device diagnostics via read-only ADB queries and output
a clean Markdown block ready to paste into a GitHub Compatibility Report.

Privacy guarantee:
  Never queries or prints serial numbers, MAC addresses, IP addresses, or accounts.

Options:
  TV_IP            Optional TV IP address (never printed in output)
  --serial SERIAL  Target a specific ADB device serial (never printed in output)
  --dry-run        Print the read-only ADB queries that would run without connecting
  -h, --help       Show this help message
EOF
}

DRY_RUN=0
TV_IP=""
SERIAL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --serial)
      [[ $# -lt 2 ]] && { echo "Error: --serial requires a value." >&2; exit 1; }
      SERIAL="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      echo "Error: Unknown option '$1'." >&2
      usage >&2
      exit 1
      ;;
    *)
      if [[ -z "$TV_IP" ]]; then
        TV_IP="$1"
        shift
      else
        echo "Error: Unexpected argument '$1'." >&2
        usage >&2
        exit 1
      fi
      ;;
  esac
done

load_config "$SCRIPT_DIR"

if [[ "$DRY_RUN" -eq 1 ]]; then
  cat <<'EOF'
[DRY RUN] Read-only ADB queries that diagnose.sh runs (no device state is modified):
  1. adb -s <target> shell getprop ro.product.manufacturer / ro.product.brand / ro.product.model
  2. adb -s <target> shell getprop ro.build.version.release / ro.build.version.sdk
  3. adb -s <target> shell getprop ro.product.cpu.abi
  4. adb -s <target> shell pm has-feature android.software.leanback (fallback: pm list features)
  5. adb -s <target> shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME (fallback: pm resolve-activity)
  6. adb -s <target> shell getprop ro.config.low_ram / dalvik.vm.heapgrowthlimit / /proc/meminfo MemTotal
  7. adb -s <target> shell pm path dev.neonlauncher (mapped to Installed / Not installed)
  8. adb -s <target> shell cmd package help (probe set-home-activity) & cmd role get-role-holders android.app.role.HOME (read-only)
[DRY RUN] No network connection or device query was executed.
EOF
  exit 0
fi

require_cmd adb "Install Android Platform Tools and ensure 'adb' is in PATH."
resolve_adb_target 0 "$TV_IP" "$SERIAL" 1

get_prop() {
  local prop_name="$1"
  local val
  val="$(adb_shell getprop "$prop_name" 2>/dev/null || true)"
  echo "${val:-unknown}"
}

MANUFACTURER="$(get_prop ro.product.manufacturer)"
BRAND="$(get_prop ro.product.brand)"
MODEL="$(get_prop ro.product.model)"
ANDROID_REL="$(get_prop ro.build.version.release)"
ANDROID_SDK="$(get_prop ro.build.version.sdk)"
PRIMARY_ABI="$(get_prop ro.product.cpu.abi)"
LOW_RAM="$(get_prop ro.config.low_ram)"
HEAP_LIMIT="$(get_prop dalvik.vm.heapgrowthlimit)"

MEM_TOTAL_KB="$(adb_shell grep '^MemTotal:' /proc/meminfo 2>/dev/null | awk '{print $2}' || true)"
if [[ "$MEM_TOTAL_KB" =~ ^[0-9]+$ ]]; then
  RAM_MB="$((MEM_TOTAL_KB / 1024)) MiB"
else
  RAM_MB="unknown"
fi

LEANBACK="$(adb_shell pm has-feature android.software.leanback 2>/dev/null || true)"
if [[ "$LEANBACK" != "true" && "$LEANBACK" != "false" ]]; then
  if adb_shell pm list features 2>/dev/null | grep -q 'android\.software\.leanback'; then
    LEANBACK="true"
  else
    LEANBACK="false"
  fi
fi

if adb_shell pm path "$APP_PACKAGE" 2>/dev/null | grep -q '^package:'; then
  NEON_STATUS="Installed"
else
  NEON_STATUS="Not installed"
fi

HOME_LAUNCHERS="$(detect_home_launchers || true)"

CMD_PKG_HELP="$(adb_shell cmd package help 2>/dev/null || true)"
if printf '%s\n' "$CMD_PKG_HELP" | grep -q 'set-home-activity'; then
  SET_HOME_SUPPORT="Supported (reported by cmd package help)"
else
  SET_HOME_SUPPORT="Unsupported or unavailable"
fi

CMD_ROLE_OUT="$(adb_shell cmd role get-role-holders android.app.role.HOME 2>&1 || true)"
if printf '%s\n' "$CMD_ROLE_OUT" | grep -qiE "Can't find service|Unknown service|not found|inaccessible"; then
  ROLE_SUPPORT="Unsupported (role service unavailable)"
elif [[ -n "$CMD_ROLE_OUT" ]] || adb_shell cmd role help >/dev/null 2>&1; then
  ROLE_SUPPORT="Supported (read-only probe succeeded)"
else
  ROLE_SUPPORT="Unknown / Unsupported"
fi

REPORT="$(
  cat <<EOF
\`\`\`markdown
### Neon Launcher Device Diagnostics

- **Manufacturer / Brand / Model**: ${MANUFACTURER} / ${BRAND} / ${MODEL}
- **Android Version / API**: Android ${ANDROID_REL} (API ${ANDROID_SDK})
- **Primary ABI**: ${PRIMARY_ABI}
- **Leanback Feature (\`android.software.leanback\`)**: ${LEANBACK}
- **Low RAM (\`ro.config.low_ram\`) / RAM / Heap Limit**: ${LOW_RAM} / ${RAM_MB} / ${HEAP_LIMIT}
- **Neon Launcher (\`${APP_PACKAGE}\`)**: ${NEON_STATUS}
- **Detected HOME Launchers**:
EOF
  if [[ -n "$HOME_LAUNCHERS" ]]; then
    while IFS= read -r comp; do
      [[ -n "$comp" ]] && printf '  - `%s`\n' "$comp"
    done <<< "$HOME_LAUNCHERS"
  else
    printf '  - `(none detected)`\n'
  fi
  cat <<EOF
- **ADB Default-Home Capability Probes (read-only)**:
  - \`cmd package set-home-activity\`: ${SET_HOME_SUPPORT}
  - \`cmd role (android.app.role.HOME)\`: ${ROLE_SUPPORT}
\`\`\`
EOF
)"

printf '%s\n' "$REPORT" | sanitize_text
