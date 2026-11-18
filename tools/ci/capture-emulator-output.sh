#!/usr/bin/env bash
set -euo pipefail
diag_dir="${POOYAN_DIAGNOSTICS_DIR:-${RUNNER_TEMP:-/tmp}/pooyan-tv-diagnostics}"
mkdir -p "$diag_dir"
emulator_output="$diag_dir/emulator-output.log"
: > "$emulator_output"
sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
emulator_bin="${sdk_root:+$sdk_root/emulator/emulator}"
if [[ -z "$emulator_bin" || ! -x "$emulator_bin" ]]; then
  echo "Android emulator executable was not found under ANDROID_SDK_ROOT/ANDROID_HOME" >&2
  exit 1
fi
real_bin="${emulator_bin}.pooyan-real"
if [[ ! -e "$real_bin" ]]; then mv "$emulator_bin" "$real_bin"; fi
cat > "$emulator_bin" <<WRAPPER
#!/usr/bin/env bash
set -uo pipefail
exec "$real_bin" "\$@" >> "$emulator_output" 2>&1
WRAPPER
chmod +x "$emulator_bin"
echo "Wrapped emulator executable: $emulator_bin" >> "$emulator_output"
