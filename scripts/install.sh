#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_common.sh
source "$SCRIPT_DIR/_common.sh"

usage() {
  cat <<'EOF'
Usage: install.sh [TV_IP] [--serial SERIAL] [--apk PATH] [--dry-run] [-h|--help]

Safely install Neon Launcher onto an Android TV / Google TV device via ADB.
Downloads the latest GitHub Release APK and verifies its SHA-256 checksum
unless --apk is provided.

Options:
  TV_IP            Optional TV IP address (defaults to port 5555 if omitted)
  --serial SERIAL  Target a specific ADB device serial
  --apk PATH       Install a local APK instead of downloading from GitHub Releases
  --dry-run        Print planned actions and rollback command without changing anything
  -h, --help       Show this help message
EOF
}

DRY_RUN=0
TV_IP=""
SERIAL=""
LOCAL_APK=""

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
    --apk)
      [[ $# -lt 2 ]] && { echo "Error: --apk requires a file path." >&2; exit 1; }
      LOCAL_APK="$2"
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

APK_URL="https://github.com/${GITHUB_REPO}/releases/latest/download/${APK_ASSET_NAME}"
SHA_URL="${APK_URL}.sha256"

if [[ -n "$LOCAL_APK" && ! -f "$LOCAL_APK" ]]; then
  echo "Error: Local APK file not found: $LOCAL_APK" >&2
  exit 1
fi

if [[ -z "$LOCAL_APK" && "$GITHUB_REPO" == "OWNER/neon-launcher" ]]; then
  echo "Warning: GITHUB_REPO in scripts/config.env is still set to 'OWNER/neon-launcher'." >&2
  echo "         Update scripts/config.env or pass --apk <path> for a local APK." >&2
fi

if [[ "$DRY_RUN" -eq 0 ]]; then
  require_cmd adb "Install Android Platform Tools and ensure 'adb' is in PATH."
fi

resolve_adb_target "$DRY_RUN" "$TV_IP" "$SERIAL"

ROLLBACK_CMD="adb"
if [[ "${#ADB_ARGS[@]}" -gt 0 ]]; then
  ROLLBACK_CMD="adb ${ADB_ARGS[*]}"
fi
ROLLBACK_CMD="${ROLLBACK_CMD} uninstall ${APP_PACKAGE}"

echo "=== Neon Launcher Install Plan ==="
echo "Package         : ${APP_PACKAGE}"
echo "Target device   : ${ADB_TARGET}"
if [[ -n "$LOCAL_APK" ]]; then
  echo "APK source      : Local file (${LOCAL_APK})"
else
  echo "APK source      : ${APK_URL}"
  echo "Checksum source : ${SHA_URL}"
fi
echo ""
echo "Rollback / uninstall command (printed before making any changes):"
echo "  ${ROLLBACK_CMD}"
echo ""

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[DRY RUN] Planned commands:"
  if [[ -z "$LOCAL_APK" ]]; then
    echo "  1. Download ${APK_URL} and ${SHA_URL}"
    echo "  2. Verify SHA-256 checksum of ${APK_ASSET_NAME}"
  fi
  if [[ "${#ADB_ARGS[@]}" -gt 0 ]]; then
    echo "  3. adb ${ADB_ARGS[*]} install -r <apk-path>"
  else
    echo "  3. adb install -r <apk-path>"
  fi
  echo "[DRY RUN] Complete. No downloads or device changes performed."
  exit 0
fi

TMP_DIR=""
cleanup() {
  if [[ -n "$TMP_DIR" && -d "$TMP_DIR" ]]; then
    rm -rf "$TMP_DIR"
  fi
}
trap cleanup EXIT

APK_TO_INSTALL="$LOCAL_APK"
if [[ -z "$APK_TO_INSTALL" ]]; then
  TMP_DIR="$(mktemp -d)"
  APK_TO_INSTALL="${TMP_DIR}/${APK_ASSET_NAME}"
  SHA_FILE="${TMP_DIR}/${APK_ASSET_NAME}.sha256"

  echo "Downloading release APK and SHA-256 checksum..."
  download_file "$APK_URL" "$APK_TO_INSTALL"
  download_file "$SHA_URL" "$SHA_FILE"

  EXPECTED_SHA="$(awk '{print tolower($1)}' "$SHA_FILE" | head -n 1)"
  ACTUAL_SHA="$(compute_sha256 "$APK_TO_INSTALL")"

  if [[ -z "$EXPECTED_SHA" || "$EXPECTED_SHA" != "$ACTUAL_SHA" ]]; then
    echo "Error: SHA-256 verification FAILED!" >&2
    echo "  Expected: ${EXPECTED_SHA:-<empty>}" >&2
    echo "  Actual  : ${ACTUAL_SHA}" >&2
    exit 1
  fi
  echo "SHA-256 verified: ${ACTUAL_SHA}"
elif [[ -f "${LOCAL_APK}.sha256" ]]; then
  EXPECTED_SHA="$(awk '{print tolower($1)}' "${LOCAL_APK}.sha256" | head -n 1)"
  ACTUAL_SHA="$(compute_sha256 "$LOCAL_APK")"
  if [[ -z "$EXPECTED_SHA" || "$EXPECTED_SHA" != "$ACTUAL_SHA" ]]; then
    echo "Error: Local APK SHA-256 mismatch against ${LOCAL_APK}.sha256!" >&2
    exit 1
  fi
  echo "Local SHA-256 verified: ${ACTUAL_SHA}"
fi

confirm_action "Install ${APP_PACKAGE} on target device (${ADB_TARGET})?"

if ! adb ${ADB_ARGS[@]+"${ADB_ARGS[@]}"} install -r "$APK_TO_INSTALL"; then
  echo "" >&2
  echo "Install failed. If switching between debug-fallback and release-signed APKs," >&2
  echo "run '${ROLLBACK_CMD}' first and retry (see docs/TROUBLESHOOTING.md)." >&2
  exit 1
fi

echo ""
echo "Neon Launcher installed successfully."
echo "Next steps:"
echo "  - Open Neon Launcher on your TV or run ./scripts/set-default-launcher.sh --dry-run"
echo "  - To uninstall later: ${ROLLBACK_CMD}"
