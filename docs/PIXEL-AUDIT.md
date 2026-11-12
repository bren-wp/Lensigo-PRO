# LENSIGO — Pixel audit (real device screenshots)

Goal: verify each of 17 active mobile and 17 active tablet layouts against their corresponding user-supplied reference PNG.

Run on a device/emulator with the matching viewport and density, the same font scale (1.0), and consistent system bars. Capture actual screens with Android `adb shell screencap -p /sdcard/lensigo.png` then `adb pull /sdcard/lensigo.png`.

A reference photo is not a valid background substitute for a live sensor feed. For camera screens compare header, icons, HUD, lower controls, frames, safe insets, text, colors and positions separately from live photo content.

Install local audit dependency: `python -m pip install Pillow`.

```bash
python tools/pixel_audit.py --reference reference/mobitel/10_panorama.png --actual screenshots/panorama.png --output audit/10_panorama
```

The script refuses differing dimensions and produces a JSON report and intensified pixel-diff PNG. For an honest 1:1 claim the generated report must show `strictly_identical: true` on the appropriate deterministic/overlaid regions. Metrics for all 34 active screen/reference pairs have not yet been recorded. Genuine Android emulator screenshots exist, but not every required reference/actual pair. Do not use the design mockups as real app screenshots in README or Play assets.

## Reference features

- `izvoz/mobitel` 19 PNG files, 1080×2400 pixels
- `izvoz/tablet` 19 PNG files, 1600×2560 pixels
- Referentna launch ikonica: `01_ikona_512x512.png`
- Brending: `04_google_play_feature_1024x500.png`

Assets were provided by the user in conversation. Only text-based vector approximations have been committed; pixel-equivalent PNGs are not yet integrated in the Android resource tree. Launcher icon requires final device-level visual comparison.

## 17 aktivnih zaslona u jednoj provjeri — mobilni i tablet

```bash
python tools/pixel_audit_batch.py --reference reference/mobitel --actual screenshots/mobitel --output audit/mobitel
python tools/pixel_audit_batch.py --reference reference/tablet --actual screenshots/tablet --output audit/tablet
```

Skripta generira `summary.json`, pojedinačne razlike i **pada** ako bilo koji od 17 screenshotova nedostaje ili nije identičan. To su stroge mjere za fiksne statične scene; za live kameru potrebno je maskirati promjenjivu sliku senzora i sustavne trake. Bez 34 usporediva Android screenshotova rezultat ostaje **neprovjeren**, a ne 1:1.

Launch PNG se reproducira identično referentnom RGB sadržaju; to nije isto što i konačni prikaz ikone unutar svih maski proizvođačkih launchera.

## Mjerljiv UI-audit kamera uz lokalno maskiranje pokretne scene

Kamera prikazuje sadržaj senzora koji se mijenja čak i kad je položaj UI gumba isti. Alat `pixel_audit.py` sada dopušta **izričito zadane pravokutnike** koje je korisnik vizualno provjerio kao područja bez HUD-a. Sve izvan pravokutnika provjerava se izvornom RGB metodom. Alat odbija pravokutnike izvan ekrana i potpuno maskiran ekran.

```bash
python tools/pixel_audit.py \
  --reference reference/mobitel/05_photo.png \
  --actual screenshots/mobitel/05_photo.png \
  --output audit/mobitel/05_photo_ui \
  --ignore-rect 10 400 300 1300
```

Po potrebi se `--ignore-rect` ponavlja za više odvojenih područja. **Ne maskirati fokusni okvir, korisničke kontrole, navigaciju, preklapanja, tekstove ili mjesta gdje se sumnja na pogrešku.** U izvještaju se odvojeno bilježe `strictly_identical` (čitav ekran), `compared_regions_identical` (samo nemaskirani pikseli), broj uspoređenih i zanemarenih piksela. Maskirano područje ostaje *neprovjereno*, a podudaranje nemaskiranih piksela nije dokaz da je čitav ekran 1:1.

Za Gallery, Viewer, Editor, Settings, Privacy i About preferirati audit **bez maski** i identične medijske datoteke u referenci/aplikaciji. Batch provjera svih 19 zaslona i dalje provodi strogi test potpunog zaslona. Nijedna snimka Android aplikacije nije izmišljena niti korištena kao zamjena za stvarni emulator.

Izvorni `03_permissions.png` i `18_privacy.png` su arhivski dizajni uklonjenih ekrana; ne uključuju se u test ili README. Stvarni screenshotovi aktualne aplikacije su u `docs/screenshots/real/`, uz transparentno označen Android emulator.
