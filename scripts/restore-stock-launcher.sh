#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "$SCRIPT_DIR/_common.sh"

usage() {
  cat <<'EOF'
Usage: restore-stock-launcher.sh [TV_IP] [--serial SERIAL] [--stock-package PKG] [--stock-component COMP] [--dry-run] [-h|--help]

Safely restore the device's stock TV launcher by re-enabling any disabled stock
launcher first, disabling dev.neonlauncher/.HomeAlias, and restoring the stock
HOME role/activity.

Options:
  TV_IP                   Optional TV IP address (defaults to port 5555)
  --serial SERIAL         Target a specific ADB device serial
  --stock-package PKG     Explicit stock launcher package name
  --stock-component COMP  Explicit stock HOME component (pkg/activity)
  --dry-run               Print planned commands without modifying the device
  -h, --help              Show this help message
EOF
}

DRY_RUN=0
TV_IP=""
SERIAL=""
STOCK_PKG=""
STOCK_COMP=""

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
    --stock-package)
      [[ $# -lt 2 ]] && { echo "Error: --stock-package requires a package name." >&2; exit 1; }
      STOCK_PKG="$2"
      shift 2
      ;;
    --stock-component)
      [[ $# -lt 2 ]] && { echo "Error: --stock-component requires pkg/activity." >&2; exit 1; }
      STOCK_COMP="$2"
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

if [[ -n "$STOCK_COMP" && -z "$STOCK_PKG" ]]; then
  STOCK_PKG="${STOCK_COMP%%/*}"
fi

if [[ "$DRY_RUN" -eq 0 ]]; then
  require_cmd adb "Install Android Platform Tools and ensure 'adb' is in PATH."
fi

resolve_adb_target "$DRY_RUN" "$TV_IP" "$SERIAL"

ADB_CMD="adb"
if [[ "${#ADB_ARGS[@]}" -gt 0 ]]; then
  ADB_CMD="adb ${ADB_ARGS[*]}"
fi

API_LEVEL=0
DISABLED_STOCK_FOUND=0

if [[ "$DRY_RUN" -eq 0 ]]; then
  SDK_RAW="$(adb_shell getprop ro.build.version.sdk 2>/dev/null || true)"
  if [[ "$SDK_RAW" =~ ^[0-9]+$ ]]; then
    API_LEVEL="$SDK_RAW"
  fi

  DISABLED_PKGS=()
  while IFS= read -r line; do
    [[ -n "$line" ]] && DISABLED_PKGS+=("$line")
  done < <(adb_shell pm list packages -d 2>/dev/null | sed 's/^package://' | grep -vi "^${APP_PACKAGE}$" || true)

  if [[ -n "$STOCK_PKG" ]]; then
    if [[ "${#DISABLED_PKGS[@]}" -gt 0 ]]; then
      for dp in "${DISABLED_PKGS[@]}"; do
        if [[ "$dp" == "$STOCK_PKG" ]]; then
          DISABLED_STOCK_FOUND=1
        fi
      done
    fi
  else
    # Check disabled launcher packages first (common TV launchers or *launcher* packages)
    DISABLED_LAUNCHERS=()
    if [[ "${#DISABLED_PKGS[@]}" -gt 0 ]]; then
      while IFS= read -r line; do
        [[ -n "$line" ]] && DISABLED_LAUNCHERS+=("$line")
      done < <(printf '%s\n' "${DISABLED_PKGS[@]}" | grep -iE 'tvlauncher|launcherx|tv\.launcher|launcher' || true)
    fi
    if [[ "${#DISABLED_LAUNCHERS[@]}" -eq 1 ]]; then
      STOCK_PKG="${DISABLED_LAUNCHERS[0]}"
      DISABLED_STOCK_FOUND=1
    elif [[ "${#DISABLED_LAUNCHERS[@]}" -gt 1 ]]; then
      echo "Multiple disabled launcher packages detected:"
      for dl in "${DISABLED_LAUNCHERS[@]}"; do
        echo "  - $dl"
      done
      echo "Re-run with --stock-package <package> to choose which one to re-enable." >&2
      exit 1
    fi
  fi

  # Also check active HOME launchers if stock package/component is still unknown
  if [[ -z "$STOCK_COMP" ]]; then
    ACTIVE_HOMES=()
    while IFS= read -r line; do
      [[ -n "$line" ]] && ACTIVE_HOMES+=("$line")
    done < <(detect_home_launchers | grep -v "^${APP_PACKAGE}/" || true)

    if [[ -n "$STOCK_PKG" && "${#ACTIVE_HOMES[@]}" -gt 0 ]]; then
      for ah in "${ACTIVE_HOMES[@]}"; do
        if [[ "${ah%%/*}" == "$STOCK_PKG" ]]; then
          STOCK_COMP="$ah"
          break
        fi
      done
    elif [[ "${#ACTIVE_HOMES[@]}" -ge 1 ]]; then
      STOCK_COMP="${ACTIVE_HOMES[0]}"
      STOCK_PKG="${STOCK_COMP%%/*}"
    fi
  fi
fi

DISPLAY_PKG="${STOCK_PKG:-<stock-launcher-pkg>}"
DISPLAY_COMP="${STOCK_COMP:-<stock-launcher-pkg>/<stock-launcher-activity>}"

echo "=== Neon Launcher Restore Stock HOME Plan ==="
echo "Target device      : ${ADB_TARGET}"
echo "Neon HomeAlias     : ${HOME_ALIAS}"
echo "Stock package      : ${DISPLAY_PKG}"
echo "Stock component    : ${DISPLAY_COMP}"
echo ""
echo "Planned commands (re-enables stock launcher BEFORE disabling HomeAlias):"
if [[ "$DRY_RUN" -eq 1 || "$DISABLED_STOCK_FOUND" -eq 1 || -n "$STOCK_PKG" ]]; then
  echo "  1. ${ADB_CMD} shell pm enable ${DISPLAY_PKG}"
fi
echo "  2. ${ADB_CMD} shell pm disable-user --user 0 ${HOME_ALIAS}"
if [[ "$DRY_RUN" -eq 1 || -n "$STOCK_COMP" ]]; then
  echo "  3. ${ADB_CMD} shell cmd package set-home-activity ${DISPLAY_COMP}"
fi
if [[ "$DRY_RUN" -eq 1 || ( "$API_LEVEL" -ge 29 && -n "$STOCK_PKG" ) ]]; then
  echo "  4. ${ADB_CMD} shell cmd role add-role-holder android.app.role.HOME ${DISPLAY_PKG}   (API 29+)"
fi
echo ""
echo "Command to switch back to Neon Launcher later:"
echo "  ${ADB_CMD} shell pm enable ${HOME_ALIAS}"
echo "  ./scripts/set-default-launcher.sh"
echo ""

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[DRY RUN] Complete. No device settings were modified."
  exit 0
fi

confirm_action "Restore stock launcher and disable ${HOME_ALIAS} on ${ADB_TARGET}?"

if [[ -n "$STOCK_PKG" ]]; then
  echo "Ensuring stock launcher ${STOCK_PKG} is enabled..."
  adb_shell pm enable "$STOCK_PKG" || adb_shell pm enable --user 0 "$STOCK_PKG" || true
fi

echo "Disabling ${HOME_ALIAS}..."
if ! adb_shell pm disable-user --user 0 "$HOME_ALIAS"; then
  adb_shell pm disable "$HOME_ALIAS" || true
fi

# Re-probe stock HOME component now that any disabled stock launcher was re-enabled
if [[ -z "$STOCK_COMP" ]]; then
  PROBED_HOMES=()
  while IFS= read -r line; do
    [[ -n "$line" ]] && PROBED_HOMES+=("$line")
  done < <(detect_home_launchers | grep -v "^${APP_PACKAGE}/" || true)

  if [[ -n "$STOCK_PKG" && "${#PROBED_HOMES[@]}" -gt 0 ]]; then
    for ph in "${PROBED_HOMES[@]}"; do
      if [[ "${ph%%/*}" == "$STOCK_PKG" ]]; then
        STOCK_COMP="$ph"
        break
      fi
    done
  elif [[ "${#PROBED_HOMES[@]}" -ge 1 ]]; then
    STOCK_COMP="${PROBED_HOMES[0]}"
    STOCK_PKG="${STOCK_COMP%%/*}"
  fi
fi

if [[ -n "$STOCK_COMP" ]]; then
  echo "Restoring HOME activity to ${STOCK_COMP}..."
  adb_shell cmd package set-home-activity "$STOCK_COMP" || true
fi

if [[ "$API_LEVEL" -ge 29 && -n "$STOCK_PKG" ]]; then
  echo "Restoring HOME role holder to ${STOCK_PKG} (API ${API_LEVEL})..."
  adb_shell cmd role add-role-holder android.app.role.HOME "$STOCK_PKG" || true
fi

echo "Launching HOME intent..."
adb_shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null 2>&1 || true

echo ""
echo "Stock launcher restore complete. Press HOME on your remote to confirm."
echo "To re-enable Neon Launcher later: ${ADB_CMD} shell pm enable ${HOME_ALIAS}"
