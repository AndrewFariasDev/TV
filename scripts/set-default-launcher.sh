#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "$SCRIPT_DIR/_common.sh"

usage() {
  cat <<'EOF'
Usage: set-default-launcher.sh [TV_IP] [--serial SERIAL] [--disable-stock] [--dry-run] [-h|--help]

Set Neon Launcher (dev.neonlauncher/.HomeAlias) as the default HOME launcher
using non-destructive ADB commands by default.

Options:
  TV_IP            Optional TV IP address (defaults to port 5555)
  --serial SERIAL  Target a specific ADB device serial
  --disable-stock  LAST RESORT: Also disable the detected stock launcher via pm disable-user.
                   Requires double warning and double confirmation.
  --dry-run        Print planned commands and rollback instructions without changing anything
  -h, --help       Show this help message
EOF
}

DRY_RUN=0
DISABLE_STOCK=0
TV_IP=""
SERIAL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --disable-stock)
      DISABLE_STOCK=1
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

if [[ "$DRY_RUN" -eq 0 ]]; then
  require_cmd adb "Install Android Platform Tools and ensure 'adb' is in PATH."
fi

resolve_adb_target "$DRY_RUN" "$TV_IP" "$SERIAL"

ADB_CMD="adb"
if [[ "${#ADB_ARGS[@]}" -gt 0 ]]; then
  ADB_CMD="adb ${ADB_ARGS[*]}"
fi

STOCK_COMPONENT="<stock-launcher-pkg>/<stock-launcher-activity>"
STOCK_PACKAGE="<stock-launcher-pkg>"
API_LEVEL=0

if [[ "$DRY_RUN" -eq 0 ]]; then
  if ! adb_shell pm path "$APP_PACKAGE" 2>/dev/null | grep -q '^package:'; then
    echo "Error: ${APP_PACKAGE} is not installed on ${ADB_TARGET}. Run ./scripts/install.sh first." >&2
    exit 1
  fi

  SDK_RAW="$(adb_shell getprop ro.build.version.sdk 2>/dev/null || true)"
  if [[ "$SDK_RAW" =~ ^[0-9]+$ ]]; then
    API_LEVEL="$SDK_RAW"
  fi

  DETECTED_HOMES=()
  while IFS= read -r line; do
    [[ -n "$line" ]] && DETECTED_HOMES+=("$line")
  done < <(detect_home_launchers | grep -v "^${APP_PACKAGE}/" || true)

  if [[ "${#DETECTED_HOMES[@]}" -eq 1 ]]; then
    STOCK_COMPONENT="${DETECTED_HOMES[0]}"
    STOCK_PACKAGE="${STOCK_COMPONENT%%/*}"
  elif [[ "${#DETECTED_HOMES[@]}" -gt 1 ]]; then
    echo "Detected multiple candidate stock HOME launchers:"
    for h in "${DETECTED_HOMES[@]}"; do
      echo "  - $h"
    done
    if [[ "$DISABLE_STOCK" -eq 1 ]]; then
      echo "Error: Refusing --disable-stock automatically when multiple stock HOME launchers exist." >&2
      echo "       Use non-destructive mode without --disable-stock, or inspect with ./scripts/diagnose.sh." >&2
      exit 1
    fi
    STOCK_COMPONENT="${DETECTED_HOMES[0]}"
    STOCK_PACKAGE="${STOCK_COMPONENT%%/*}"
  else
    if [[ "$DISABLE_STOCK" -eq 1 ]]; then
      echo "Error: Could not detect an active stock launcher package; refusing --disable-stock for safety." >&2
      exit 1
    fi
  fi
fi

echo "=== Neon Launcher Default HOME Setup Plan ==="
echo "Target device      : ${ADB_TARGET}"
echo "Neon HomeAlias     : ${HOME_ALIAS}"
echo "Detected stock HOME: ${STOCK_COMPONENT}"
echo "Android API level  : $( [[ "$API_LEVEL" -gt 0 ]] && echo "$API_LEVEL" || echo "unknown (dry-run)" )"
echo ""
echo "Planned non-destructive commands:"
echo "  1. ${ADB_CMD} shell pm enable ${HOME_ALIAS}"
echo "  2. ${ADB_CMD} shell cmd package set-home-activity ${HOME_ALIAS}"
if [[ "$DRY_RUN" -eq 1 || "$API_LEVEL" -ge 29 ]]; then
  echo "  3. ${ADB_CMD} shell cmd role add-role-holder android.app.role.HOME ${APP_PACKAGE}   (API 29+)"
fi
if [[ "$DISABLE_STOCK" -eq 1 ]]; then
  echo "  4. [DESTRUCTIVE LAST RESORT] ${ADB_CMD} shell pm disable-user --user 0 ${STOCK_PACKAGE}"
fi
echo ""
echo "Rollback / restore commands (printed BEFORE making any changes):"
echo "  Option A (helper script):"
echo "    ./scripts/restore-stock-launcher.sh --stock-package ${STOCK_PACKAGE} --stock-component ${STOCK_COMPONENT}"
echo "  Option B (direct ADB commands):"
if [[ "$DISABLE_STOCK" -eq 1 ]]; then
  echo "    ${ADB_CMD} shell pm enable ${STOCK_PACKAGE}"
fi
echo "    ${ADB_CMD} shell pm disable-user --user 0 ${HOME_ALIAS}"
echo "    ${ADB_CMD} shell cmd package set-home-activity ${STOCK_COMPONENT}"
if [[ "$DRY_RUN" -eq 1 || "$API_LEVEL" -ge 29 ]]; then
  echo "    ${ADB_CMD} shell cmd role add-role-holder android.app.role.HOME ${STOCK_PACKAGE}"
fi
echo ""

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[DRY RUN] Complete. No device settings were modified."
  exit 0
fi

confirm_action "Apply non-destructive default HOME settings on ${ADB_TARGET}?"

if [[ "$DISABLE_STOCK" -eq 1 ]]; then
  echo ""
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "WARNING 1/2: You passed --disable-stock."
  echo "Disabling '${STOCK_PACKAGE}' removes the built-in TV fallback launcher."
  echo "If Neon Launcher is uninstalled or fails while disabled, press HOME"
  echo "recovery requires ADB."
  echo ""
  echo "Exact command to re-enable your stock launcher:"
  echo "  ${ADB_CMD} shell pm enable ${STOCK_PACKAGE}"
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  confirm_action "Confirm (1/2) you understand the risk of disabling ${STOCK_PACKAGE}?"

  echo ""
  echo "WARNING 2/2: Final confirmation before running 'pm disable-user --user 0 ${STOCK_PACKAGE}'."
  echo "Re-enable command: ${ADB_CMD} shell pm enable ${STOCK_PACKAGE}"
  confirm_action "Confirm (2/2) disable ${STOCK_PACKAGE} now?"
fi

echo "Enabling ${HOME_ALIAS}..."
adb_shell pm enable "$HOME_ALIAS" || true

echo "Setting HOME activity via cmd package set-home-activity..."
if ! adb_shell cmd package set-home-activity "$HOME_ALIAS"; then
  echo "Note: 'cmd package set-home-activity' failed or is unsupported on this firmware."
fi

if [[ "$API_LEVEL" -ge 29 ]]; then
  echo "Adding HOME role holder via cmd role (API ${API_LEVEL})..."
  if ! adb_shell cmd role add-role-holder android.app.role.HOME "$APP_PACKAGE"; then
    echo "Note: 'cmd role add-role-holder' failed or is restricted on this firmware."
  fi
fi

if [[ "$DISABLE_STOCK" -eq 1 ]]; then
  echo "Disabling stock launcher ${STOCK_PACKAGE} for user 0..."
  adb_shell pm disable-user --user 0 "$STOCK_PACKAGE"
  echo "Reminder: To re-enable later, run: ${ADB_CMD} shell pm enable ${STOCK_PACKAGE}"
fi

echo ""
echo "Done. Press the HOME button on your TV remote to verify."
echo "If the TV still opens the vendor launcher, review docs/INSTALL.md and docs/COMPATIBILITY.md."
