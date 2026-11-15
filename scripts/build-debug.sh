#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
sh scripts/verify-offline.sh
if [ ! -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/missing-android-sdk}}" ]; then
    echo 'Android SDK not found; set ANDROID_HOME or ANDROID_SDK_ROOT.' >&2
    exit 2
fi
if ! command -v gradle >/dev/null 2>&1; then
    echo 'Gradle 8.9 not installed; use GitHub Actions or Android Studio.' >&2
    exit 2
fi
gradle --no-daemon assembleDebug
printf '\nAPK location: %s\n' "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"
