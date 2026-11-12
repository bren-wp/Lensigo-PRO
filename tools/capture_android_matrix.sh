#!/usr/bin/env bash
# Interactive, non-destructive Android screenshot acquisition.
# The user navigates each actual screen before pressing Enter.
set -euo pipefail
if [[ $# -ne 2 || ( "$1" != "mobitel" && "$1" != "tablet" ) ]]; then
  echo "Usage: bash tools/capture_android_matrix.sh mobitel|tablet screenshots"
  exit 2
fi
command -v adb >/dev/null || { echo "Android platform-tools (adb) required"; exit 2; }
device="$1"; root="$2/$device"
mkdir -p "$root"
screens=(01_splash 02_onboarding 04_home 05_photo 06_video
  07_pro 08_night 09_portrait 10_panorama 11_dual 12_filters
  13_gallery 14_viewer 15_editor 16_settings 17_camera-settings
  19_about)
echo "Prepare the $device emulator/device, reference font scale, density and system bars."
echo "Capture only real Android application screens, not design mockups."
for screen in "${screens[@]}"; do
  read -r -p "Open $screen on the Android device, then press Enter (Ctrl+C to stop): "
  adb exec-out screencap -p > "$root/$screen.png"
  test -s "$root/$screen.png"
  echo "Saved $root/$screen.png"
done
echo "All 17 active-screen captures saved in $root"
