> **Aktualizacija, studeni 2026.:** od 19 izvornih konceptnih zaslona uklonjeni su 03 Permissions i 18 Privacy. Trenutačno je aktivno 17 prikaza po tipu uređaja. Dizajnerski koncepti nisu stvarni screenshotovi. Kamera i mikrofon i dalje koriste Androidove sistemske dozvole kada je potrebno.

# LENSIGO — Consumer UI quality and pixel-contract audit

Reference source is the user-supplied 19-screen mobile/tablet Android UI package, including `prototip/index.html` and all native PNG reference images.

## Common geometry and visual vocabulary

| UI element | Reference | Android implementation |
|---|---|---|
| Brand | Cyan aperture on midnight navy; LENSIGO white and PRO CAMERA cyan | Local vector and verified launcher, fixed typography/colors |
| Home | Hero at top, eight two-column 105dp mode tiles, single gallery CTA | Rebuilt 04_home Compose layout with responsive cards, matching labels and actions |
| Settings | Header 64dp, navy surface, grouped rounded panels, 22dp icons, divider rules | Rebuilt screens 16 and 17 with grouped rows and working controls |
| Photography | 4:3/16:9, flash, camera selection, zoom, modes, shutter | CameraX; hardware-gated functions |
| Gallery / editor | Card backgrounds and 3-column thumbnails; local save, share, crop/rotate | Separate operational screens |
| Privacy/About | Brand icon, detailed panels, live navigation and real permission entry | Screens 18 and 19 |

## Copy quality and functional controls

- No developer messages, demo/placeholder labels, promises of future functionality, or fake settings are shown in Android UI.
- Only supported photo formats JPEG and video qualities SD/HD are selectable.
- Raw/HEIF/4K options are not presented as clickable controls, because they are not provided by the current capture pipeline.
- JPEG quality, camera grid, shutter timer, shutter sound, SD/HD preference, noise reduction, sharpening and low-memory processing are bound to actual persistent settings.
- Permission controls open Android settings or trigger the runtime microphone request.
- Screen 04 home cards navigate to the corresponding live camera mode; bottom gallery button opens an actual local gallery.

## Verification still required

- Without real Android device/emulator screenshots at matching resolution, neither the 19 mobile nor the 19 tablet screens can be declared mathematically pixel-for-pixel 1:1. Image content from a live sensor differs from static concept photos.
- A pixel-diff script and 38-screen status matrix are in the repo; CI currently validates the audit logic, not full screenshot identity.
- Test hardware-specific CameraX modes, manufacturer extensions, permission revocation, low RAM, and Android 16/17 edge-to-edge behaviors on physical devices.
- The launcher source RGB pixels match the supplied 512px icon; manufacturer icon masks may differ.

Consumer UI audit in CI: `python tools/audit_public_ui.py`.
