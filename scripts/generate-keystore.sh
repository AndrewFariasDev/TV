#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=_common.sh
source "$SCRIPT_DIR/_common.sh"

usage() {
  cat <<'EOF'
Usage: generate-keystore.sh [--output PATH] [--alias ALIAS] [--validity DAYS] [--dry-run] [-h|--help]

Generate a PKCS12 release signing keystore (default: keystore/release.jks,
which is gitignored) and print instructions for configuring GitHub Actions
Secrets without writing passwords to disk.

Options:
  --output PATH    Keystore output path (default: keystore/release.jks)
  --alias ALIAS    Key alias (default: neonlauncher)
  --validity DAYS  Certificate validity in days (default: 10000)
  --dry-run        Print planned commands and secret instructions without creating files
  -h, --help       Show this help message
EOF
}

DRY_RUN=0
OUTPUT_PATH="${REPO_ROOT}/keystore/release.jks"
KEY_ALIAS_NAME="neonlauncher"
VALIDITY_DAYS="10000"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --output)
      [[ $# -lt 2 ]] && { echo "Error: --output requires a file path." >&2; exit 1; }
      OUTPUT_PATH="$2"
      shift 2
      ;;
    --alias)
      [[ $# -lt 2 ]] && { echo "Error: --alias requires a value." >&2; exit 1; }
      KEY_ALIAS_NAME="$2"
      shift 2
      ;;
    --validity)
      [[ $# -lt 2 ]] && { echo "Error: --validity requires a number of days." >&2; exit 1; }
      VALIDITY_DAYS="$2"
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
      echo "Error: Unexpected argument '$1'." >&2
      usage >&2
      exit 1
      ;;
  esac
done

load_config "$SCRIPT_DIR"

if [[ ! "$VALIDITY_DAYS" =~ ^[0-9]+$ || "$VALIDITY_DAYS" -le 0 ]]; then
  echo "Error: --validity must be a positive integer." >&2
  exit 1
fi

echo "=== Neon Launcher Keystore Generator ==="
echo "Output path  : ${OUTPUT_PATH}"
echo "Keystore type: PKCS12"
echo "Key alias    : ${KEY_ALIAS_NAME}"
echo "Validity     : ${VALIDITY_DAYS} days"
echo ""
echo "Rollback / removal command if you decide not to keep the new keystore:"
echo "  rm -f \"${OUTPUT_PATH}\""
echo ""

print_secret_instructions() {
  cat <<EOF
Next steps — Configure GitHub Actions Secrets (Settings -> Secrets and variables -> Actions):

1. Encode your keystore to single-line Base64 (without writing passwords to disk):
   Linux:
     base64 -w 0 "${OUTPUT_PATH}"
   macOS:
     base64 -i "${OUTPUT_PATH}" | tr -d '\n'

2. Create these 4 repository secrets:
   - KEYSTORE_BASE64   : (paste the single-line Base64 output from step 1)
   - KEYSTORE_PASSWORD : (the keystore password you entered interactively)
   - KEY_ALIAS         : ${KEY_ALIAS_NAME}
   - KEY_PASSWORD      : (the key password you entered interactively)

Keep "${OUTPUT_PATH}" backed up securely offline and NEVER commit it to git.
EOF
}

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[DRY RUN] Planned command:"
  echo "  mkdir -p \"$(dirname "$OUTPUT_PATH")\""
  echo "  keytool -genkeypair -v -keystore \"${OUTPUT_PATH}\" -storetype PKCS12 -alias \"${KEY_ALIAS_NAME}\" -keyalg RSA -keysize 2048 -validity \"${VALIDITY_DAYS}\""
  echo ""
  print_secret_instructions
  echo "[DRY RUN] Complete. No keystore file was created."
  exit 0
fi

require_cmd keytool "Install JDK 17 (or newer) and ensure 'keytool' is in PATH."

if [[ -e "$OUTPUT_PATH" ]]; then
  echo "WARNING: Keystore file already exists at: ${OUTPUT_PATH}"
  echo "Replacing an existing release keystore breaks seamless updates for previously signed APKs."
  confirm_action "Overwrite existing keystore at ${OUTPUT_PATH}?"
  rm -f "$OUTPUT_PATH"
else
  confirm_action "Generate new PKCS12 keystore at ${OUTPUT_PATH}?"
fi

mkdir -p "$(dirname "$OUTPUT_PATH")"

echo ""
echo "Launching keytool (passwords are entered directly to keytool and are NOT logged to disk)..."
keytool -genkeypair -v \
  -keystore "$OUTPUT_PATH" \
  -storetype PKCS12 \
  -alias "$KEY_ALIAS_NAME" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS"

chmod 600 "$OUTPUT_PATH" 2>/dev/null || true

echo ""
echo "Keystore created at: ${OUTPUT_PATH}"
echo ""
print_secret_instructions
