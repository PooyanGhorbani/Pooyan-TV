#!/usr/bin/env bash
set -uo pipefail
mode="${1:?usage: run-tv-instrumentation.sh full|dpad-only|logical-720}"
diag_dir="${POOYAN_DIAGNOSTICS_DIR:-${RUNNER_TEMP:-/tmp}/pooyan-tv-diagnostics}"
mkdir -p "$diag_dir"
case "$mode" in
  full) gradle_args=(--no-daemon connectedDebugAndroidTest) ;;
  dpad-only)
    gradle_args=(--no-daemon connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pooyan.namakadetv.TvPremiumInstrumentedTest\#zHomeHeroCarouselAndRemoteUpNavigationWork)
    ;;
  logical-720)
    adb shell wm size 1280x720 > "$diag_dir/display-size-set.log" 2>&1
    adb shell wm size > "$diag_dir/display-size-verified.log" 2>&1
    adb shell wm density > "$diag_dir/display-density-verified.log" 2>&1
    grep -Fq 'Override size: 1280x720' "$diag_dir/display-size-verified.log" || {
      cat "$diag_dir/display-size-verified.log" >&2
      echo "Logical 720p override did not take effect" >&2
      exit 2
    }
    gradle_args=(--no-daemon connectedDebugAndroidTest)
    ;;
  *) echo "Unknown mode: $mode" >&2; exit 2 ;;
esac
active=1
logcat_pid=""
watch_pid=""
cleanup() {
  active=0
  if [[ -n "$logcat_pid" ]]; then pkill -P "$logcat_pid" 2>/dev/null || true; kill "$logcat_pid" 2>/dev/null || true; wait "$logcat_pid" 2>/dev/null || true; fi
  if [[ -n "$watch_pid" ]]; then pkill -P "$watch_pid" 2>/dev/null || true; kill "$watch_pid" 2>/dev/null || true; wait "$watch_pid" 2>/dev/null || true; fi
}
trap cleanup EXIT INT TERM
adb devices -l > "$diag_dir/adb-devices-initial.log" 2>&1 || true
adb -s emulator-5554 logcat -G 16M > "$diag_dir/logcat-buffer-size.log" 2>&1 || true
adb -s emulator-5554 shell wm size > "$diag_dir/display-size-initial.log" 2>&1 || true
adb -s emulator-5554 shell wm density > "$diag_dir/display-density-initial.log" 2>&1 || true
(while [[ "$active" -eq 1 ]]; do
   printf '\n=== %s ===\n' "$(date --iso-8601=ns)"
   adb devices -l 2>&1 || true
   adb -s emulator-5554 get-state 2>&1 || true
   adb -s emulator-5554 shell getprop sys.boot_completed 2>&1 || true
   adb -s emulator-5554 shell wm size 2>&1 || true
   adb -s emulator-5554 shell pidof com.pooyan.namakadetv.dev070 2>&1 || true
   ps -eo pid,ppid,stat,etime,rss,args | grep -E '[q]emu|[e]mulator' || true
   sleep 2
 done) >> "$diag_dir/adb-qemu-watch.log" 2>&1 &
watch_pid=$!
(while [[ "$active" -eq 1 ]]; do
   adb logcat -b all -v threadtime 2>&1
   [[ "$active" -eq 1 ]] && sleep 1
 done) >> "$diag_dir/logcat-live.log" 2>&1 &
logcat_pid=$!
printf 'mode=%s\nstarted=%s\n' "$mode" "$(date --iso-8601=ns)" > "$diag_dir/test-metadata.log"
if [[ "$mode" == "dpad-only" ]]; then
  printf 'selector=TvPremiumInstrumentedTest#zHomeHeroCarouselAndRemoteUpNavigationWork\n' >> "$diag_dir/test-metadata.log"
elif [[ "$mode" == "logical-720" ]]; then
  printf 'avd_profile=tv_1080p\nlogical_size=1280x720\n' >> "$diag_dir/test-metadata.log"
fi
set +e
gradle "${gradle_args[@]}" 2>&1 | tee "$diag_dir/instrumentation-gradle.log"
gradle_status=${PIPESTATUS[0]}
set -e
printf 'finished=%s\ngradle_exit=%s\n' "$(date --iso-8601=ns)" "$gradle_status" >> "$diag_dir/test-metadata.log"
if [[ "$mode" == "full" || "$mode" == "logical-720" ]]; then
  mkdir -p "$diag_dir/previews"
  adb -s emulator-5554 pull /sdcard/Android/data/com.pooyan.namakadetv.dev070/files "$diag_dir/previews/app-files" \
    > "$diag_dir/previews/pull.log" 2>&1 || true
  for image in ultimate-home.png ultimate-home-live.png ultimate-movie-details.png ultimate-detail-menu-expanded.png ultimate-series-details.png ultimate-search.png ultimate-player.png ultimate-player-unavailable.png; do
    awk -v image="$image" '
      index($0,"UltimatePreview: PNG_BEGIN:" image ":"){active=1;next}
      index($0,"UltimatePreview: PNG_END:" image){active=0;ended=1;next}
      active { marker="UltimatePreview: PNG_CHUNK:" image ":"; at=index($0,marker); if(at){data=substr($0,at+length(marker)); sub(/^[0-9]+:/,"",data); printf "%s",data; found=1} }
      END { if(found&&ended) printf "\n" }
    ' "$diag_dir/logcat-live.log" | base64 --decode > "$diag_dir/previews/$image" 2> "$diag_dir/previews/$image.decode.log" || true
    if ! grep -q "UltimatePreview: PNG_END:$image" "$diag_dir/logcat-live.log" || [[ ! -s "$diag_dir/previews/$image" ]]; then rm -f "$diag_dir/previews/$image"; fi
  done
fi
if [[ "$mode" == "full" || "$mode" == "logical-720" ]]; then
  awk '/DetailPreview: PNG_CHUNK:[0-9][0-9][0-9][0-9]:/ { sub(/^.*DetailPreview: PNG_CHUNK:[^:]*:/, ""); printf "%s", $0; found=1 } END { if(found) printf "\n" }' "$diag_dir/logcat-live.log" | base64 --decode > "$diag_dir/emulator-detail.png" 2> "$diag_dir/screenshot-pull.log" || true
  if ! grep -q 'DetailPreview: PNG_END' "$diag_dir/logcat-live.log" || [[ ! -s "$diag_dir/emulator-detail.png" ]]; then
    printf 'No complete rendered details screenshot in captured Logcat; see %s/screenshot-pull.log\n' "$diag_dir" | tee -a "$diag_dir/test-metadata.log"
    rm -f "$diag_dir/emulator-detail.png"
  fi
  if [[ -s "$diag_dir/emulator-detail.png" ]]; then sha256sum "$diag_dir/emulator-detail.png" >> "$diag_dir/test-metadata.log"; else printf 'screenshot=missing\n' >> "$diag_dir/test-metadata.log"; fi
fi
exit "$gradle_status"
