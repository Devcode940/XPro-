#!/usr/bin/env bash
# Generates the repo-root debug.keystore that app/build.gradle.kts prefers for debug builds.
# (Building without it falls back to ~/.android/debug.keystore, which Android Studio creates.)
set -euo pipefail

KEYSTORE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/debug.keystore"

if [[ -f "$KEYSTORE" ]]; then
  echo "debug.keystore already exists at $KEYSTORE — leaving it untouched."
  exit 0
fi

keytool -genkeypair \
  -alias androiddebugkey \
  -keystore "$KEYSTORE" \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

echo "Created $KEYSTORE"
echo "Note: this file is git-ignored on purpose. Debug keystores are interchangeable; release builds need your own upload keystore (KEYSTORE_PATH/STORE_PASSWORD/KEY_PASSWORD env vars)."
