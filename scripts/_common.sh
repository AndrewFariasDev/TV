#!/usr/bin/env bash
# Shared helper functions for Neon Launcher scripts.
# Sourced by scripts/*.sh; not meant to be executed directly.

set -euo pipefail

load_config() {
  local script_dir="$1"
  local env_file="$script_dir/config.env"
  if [[ ! -f "$env_file" ]]; then
    echo "Error: Missing configuration file: $env_file" >&2
    return 1
  fi

  local line key val
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      val="${BASH_REMATCH[2]}"
      val="${val#\"}"
      val="${val%\"}"
      val="${val#\'}"
      val="${val%\'}"
      case "$key" in
        GITHUB_REPO) GITHUB_REPO="$val" ;;
        APP_PACKAGE) APP_PACKAGE="$val" ;;
        HOME_ALIAS) HOME_ALIAS="$val" ;;
        MAIN_ACTIVITY) MAIN_ACTIVITY="$val" ;;
        APK_ASSET_NAME) APK_ASSET_NAME="$val" ;;
      esac
    fi
  done < "$env_file"

  GITHUB_REPO="${GITHUB_REPO:-OWNER/neon-launcher}"
  APP_PACKAGE="${APP_PACKAGE:-dev.neonlauncher}"
  HOME_ALIAS="${HOME_ALIAS:-dev.neonlauncher/.HomeAlias}"
  MAIN_ACTIVITY="${MAIN_ACTIVITY:-dev.neonlauncher/.MainActivity}"
  APK_ASSET_NAME="${APK_ASSET_NAME:-neon-launcher.apk}"
}

require_cmd() {
  local cmd="$1"
  local hint="${2:-}"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Error: Required command '$cmd' was not found in PATH." >&2
    if [[ -n "$hint" ]]; then
      echo "Hint: $hint" >&2
    fi
    return 1
  fi
}

confirm_action() {
  local prompt="$1"
  local reply=""
  if [[ -t 0 ]]; then
    read -r -p "$prompt [y/N]: " reply
  elif [[ -r /dev/tty ]]; then
    read -r -p "$prompt [y/N]: " reply < /dev/tty
  else
    echo "Error: Cannot prompt for confirmation in a non-interactive shell." >&2
    return 1
  fi

  case "$reply" in
    y|Y|yes|YES) return 0 ;;
    *)
      echo "Aborted by user. No changes were made."
      return 1
      ;;
  esac
}

resolve_adb_target() {
  local dry_run="$1"
  local ip_arg="$2"
  local serial_arg="$3"
  local silent="${4:-0}"

  ADB_TARGET=""
  ADB_ARGS=()

  if [[ -n "$serial_arg" && -n "$ip_arg" ]]; then
    echo "Error: Specify either a TV IP address OR --serial, not both." >&2
    return 1
  fi

  if [[ -n "$serial_arg" ]]; then
    ADB_TARGET="$serial_arg"
    ADB_ARGS=(-s "$ADB_TARGET")
    return 0
  fi

  if [[ -n "$ip_arg" ]]; then
    local target_ip="$ip_arg"
    if [[ "$target_ip" != *:* ]]; then
      target_ip="${target_ip}:5555"
    fi
    ADB_TARGET="$target_ip"
    ADB_ARGS=(-s "$ADB_TARGET")
    if [[ "$dry_run" -eq 0 ]]; then
      if [[ "$silent" -eq 1 ]]; then
        adb connect "$ADB_TARGET" >/dev/null 2>&1 || true
      else
        adb connect "$ADB_TARGET"
      fi
      local state
      state="$(adb "${ADB_ARGS[@]}" get-state 2>/dev/null | tr -d '\r' || true)"
      if [[ "$state" != "device" ]]; then
        echo "Error: Target ADB device is not ready (state: '${state:-offline}'). Check TV debugging and authorization prompt." >&2
        return 1
      fi
    fi
    return 0
  fi

  if [[ "$dry_run" -eq 1 ]]; then
    ADB_TARGET="<device>"
    ADB_ARGS=()
    return 0
  fi

  local -a devices=()
  local dev_line dev_id dev_state
  while IFS= read -r dev_line; do
    dev_line="${dev_line%$'\r'}"
    [[ -z "$dev_line" || "$dev_line" == List\ of\ devices* ]] && continue
    dev_id="${dev_line%%[[:space:]]*}"
    dev_state="${dev_line#"$dev_id"}"
    dev_state="${dev_state#"${dev_state%%[![:space:]]*}"}"
    if [[ "$dev_state" == device* ]]; then
      devices+=("$dev_id")
    fi
  done < <(adb devices 2>/dev/null || true)

  if [[ "${#devices[@]}" -eq 0 ]]; then
    echo "Error: No connected ADB device found. Pass a TV IP address or --serial <id>." >&2
    return 1
  elif [[ "${#devices[@]}" -gt 1 ]]; then
    echo "Error: Multiple ADB devices connected (${#devices[@]}). Pass a TV IP address or --serial <id>." >&2
    return 1
  fi

  ADB_TARGET="${devices[0]}"
  ADB_ARGS=(-s "$ADB_TARGET")
  return 0
}

adb_shell() {
  adb ${ADB_ARGS[@]+"${ADB_ARGS[@]}"} shell "$@" | tr -d '\r'
}

sanitize_text() {
  sed -E \
    -e 's/[0-9]{1,3}(\.[0-9]{1,3}){3}/[REDACTED_IP]/g' \
    -e 's/([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}/[REDACTED_MAC]/g' \
    -e 's/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/[REDACTED_EMAIL]/g'
}

parse_launcher_components() {
  awk '
    /^[a-zA-Z0-9._]+\/[a-zA-Z0-9._$]+$/ {
      if (!seen[$0]++) print $0
      next
    }
    /(^|[[:space:]])packageName=/ {
      val = $0
      sub(/^.*packageName=/, "", val)
      sub(/[[:space:]].*$/, "", val)
      pkg = val
      next
    }
    /(^|[[:space:]])name=/ {
      val = $0
      sub(/^.*name=/, "", val)
      sub(/[[:space:]].*$/, "", val)
      if (val ~ /\./) {
        cls = val
      }
      next
    }
    /ActivityInfo:|Activity #[0-9]+:/ {
      if (pkg != "" && cls != "") {
        comp = pkg "/" cls
        if (!seen[comp]++) print comp
      }
      pkg = ""
      cls = ""
    }
    END {
      if (pkg != "" && cls != "") {
        comp = pkg "/" cls
        if (!seen[comp]++) print comp
      }
    }
  '
}

detect_home_launchers() {
  local raw=""
  local parsed=""

  raw="$(adb_shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null || true)"
  parsed="$(printf '%s\n' "$raw" | parse_launcher_components)"

  if [[ -z "$parsed" ]]; then
    raw="$(adb_shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null || true)"
    parsed="$(printf '%s\n' "$raw" | parse_launcher_components)"
  fi

  if [[ -z "$parsed" ]]; then
    raw="$(adb_shell pm resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null || true)"
    parsed="$(printf '%s\n' "$raw" | parse_launcher_components)"
  fi

  if [[ -z "$parsed" ]]; then
    raw="$(adb_shell pm resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null || true)"
    parsed="$(printf '%s\n' "$raw" | parse_launcher_components)"
  fi

  printf '%s\n' "$parsed" | sed '/^$/d'
}

compute_sha256() {
  local file_path="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file_path" | awk '{print tolower($1)}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file_path" | awk '{print tolower($1)}'
  elif command -v openssl >/dev/null 2>&1; then
    openssl dgst -sha256 "$file_path" | awk '{print tolower($NF)}'
  else
    echo "Error: No SHA-256 tool found (install sha256sum, shasum, or openssl)." >&2
    return 1
  fi
}

download_file() {
  local url="$1"
  local dest="$2"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL --retry 2 -o "$dest" "$url"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$dest" "$url"
  else
    echo "Error: Neither 'curl' nor 'wget' is available to download files." >&2
    return 1
  fi
}
