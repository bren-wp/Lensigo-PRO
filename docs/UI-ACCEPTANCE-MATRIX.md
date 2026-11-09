# LENSIGO — acceptance matrix for all 38 reference screens

This pipeline **never claims 1:1 without an emulator or a physical device**.
It requires 19 genuine application screenshots for the 1080×2400 mobile
reference and 19 for the 1600×2560 tablet reference.

## Collect screenshots (local Android SDK + device required)

On the correct Android emulator/device, set the same viewport, display
density, system insets, navigation mode and font scale as the references.
Install the actual LENSIGO APK, grant the camera permissions, and navigate
each real screen. Then run the interactive capture tool:

```bash
bash tools/capture_android_matrix.sh mobitel screenshots
bash tools/capture_android_matrix.sh tablet screenshots
```

The script does **not** synthesize screenshots. For each screen it waits
until a person or a separate UI test has navigated the app there.

## Generate ranked deviations

```bash
python tools/ui_acceptance_matrix.py \
    --reference-root reference/izvoz \
    --actual-root screenshots \
    --output-root audit
```

Outputs: `audit/acceptance.json`, `audit/largest_deviations.csv`, both
device summaries, and 38 full-resolution pixel-diff PNG files.

If screenshots are missing, mismatched in size, or otherwise unreadable,
the audit **fails**. Largest-deviation rankings are calculated only for
verified screenshot pairs. UI overlays must be analyzed separately from
changing sensor scenes; do not report those areas as 0% deviation or erase
unfavorable controls with broad masks.

## Release acceptance gates

- All visible controls trigger real functionality, hardware-gated when needed.
- Raw DNG only for CameraX-supported sensor/format combinations.
- 60 FPS and video stabilization indicate target/availability; actual codec
  behavior is validated from recorded file metadata on physical devices.
- Portrait person mask and panorama perspective match tested on diverse scenes.
- Gallery/Viewer/Editor/Settings/Privacy/About tested at API 24–36.
- AAB signed with production upload key; current preview AAB is **unsigned**.
- Full pixel-perfect 1:1 can be claimed only for the exactly matched
  screen/viewport pairs whose full-frame diff supports it.
