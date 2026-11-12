> **Aktualni opseg:** 17 zaslona na mobitelu i 17 na tabletu. Zasebni ekrani Privatnost i Dopuštenja uklonjeni su. Za foto/video funkcije ostaju obvezne Androidove native dozvole. Stvarna README galerija koristi Android emulator screencap.

# LENSIGO roadmap

## 0.1.x
- Real CameraX photo/video capture, local media and front/back switching
- Reference-based branded navigation
- CI previews

## Next
- Storage runtime permission path for Android 7–9
- Pixel-level screen-by-screen UI validation against user reference PNGs
- Real gallery and photo viewer/editor with favorites, crop/rotate/share/delete (implemented; physical validation pending)
- Image capture controls and capability detection
- Pro Camera2 ISO/shutter/manual focus (done with hardware gating; physical device validation pending)
- Night/Portrait manufacturer extensions and local fallback (implemented, device validation pending); guided panorama and dual stills (implemented, device tests pending)
- Photometric/perspective automatic panorama alignment and concurrent video (not yet implemented)
- Accessible responsive layouts and localized HR/EN strings
- Signed production AAB/APK using secure CI secrets; tests and Play compliance

No future functionality is represented as complete.

## Implemented in 0.6.0

- Privacy/About layouts and active navigation, actual version displayed, licence page.
- Android runtime permission refresh on resume, back navigation and cancellable shutter timer.
- Restrict MediaStore gallery listing by app folder/prefix; low-memory thumbnail decode on Android 7–9.
- Tests for neutral photo looks and deterministic screenshot comparisons.

**Still required:** instrumented screenshots against all 38 reference PNGs, physical hardware testing, automatic panorama alignment, selective portrait masking, localization HR/EN and release signing. A green Gradle build alone is not a production certification.
