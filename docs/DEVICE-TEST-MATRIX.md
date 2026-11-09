# LENSIGO — device/visual test matrix

The feature is **not considered device-tested** just because it compiles.

| Test configuration | Required checks | Result |
|---|---|---|
| Android 7 / API 24, 360 dp narrow | permissions incl. storage; photo JPEG; SD video; gallery ownership; controls not clipped | Not run |
| Android 9 / API 28, low RAM | noise reduction, portrait fallback, panorama 2–5 shots, release buffers on exit | Not run |
| Android 10 / API 29 | MediaStore owner paths, deleting owned media, app preferences | Not run |
| Android 12 / API 31 | camera live effects, Night/Bokeh extensions only when supported | Not run |
| Android 13–16 / API 33–36 | scoped media, edge-to-edge bars, system permission roundtrip | Not run |
| 1080 × 2400 pixel phone | compare 18 active screens to `izvoz/mobitel`; original 18_privacy archived | No screenshots yet |
| 1600 × 2560 pixel tablet | compare 18 active screens to `izvoz/tablet`; original 18_privacy archived | No screenshots yet |
| Landscape and large text | scrollability, no truncated buttons, 1.3×/2× font scaling | Not run |

## Screens 18 and 19

- Privacy: no separate screen; confirm Camera/Settings permission states and offline storage remain operational.
- About: centered symbol / LENSIGO wordmark, four rows (app name, website, real version, licences), copyright and version.
- Buttons are wired to Android application settings, licence information, or the external browser via ACTION_VIEW.
- All screen copies use meaningful icons and avoid representing future features as complete.

## Security and stability checks

- No INTERNET permission, third-party analytics or advertising SDK.
- Avoid broad media read permissions; the gallery selects LENSIGO file prefix and known MediaStore folder (Android 10+).
- On OS-settings resume, refresh microphone/camera/storage permission state.
- Cancel countdown on changing camera mode or leaving the camera, preventing delayed capture.
- Delete through Android's confirmation request on API 30+.
- Close streams, recycle decoded bitmaps, remove partial output on errors.
- No automatic backups for local app preferences (android:allowBackup=false).
- Manual tests for permission revocation, app backgrounding, slow storage, low storage, camera unavailable, camera switching, and vendor extension fallbacks remain necessary.

A screenshot score is valid only for the specific device settings used to capture the screenshot, and camera-content pixels should be masked when comparing overlay geometry. No general claim of pixel-for-pixel visual equivalence is made until per-screen reports are recorded.

## Upgrade 0.7.0

- Provjeriti navigaciju na početnoj mreži (8 kartica) pri 320–600 dp, na velikim fontovima i tabletima.
- Izvoz uređivača usporediti s istim crop/rotate/filter previewom za fotografije s EXIF zrcaljenjem.
- Ograničiti i profilirati memoriju video minijatura API 24–28.
- Provjera APK/AAB native knjižnica prije svakog novog izdanja: `tools/play_package_check.py`.
- Android 17 / compileSdk 37 uz targetSdk 36, Google Play 2026 i 16 KB provjeru. Ponašanje na stvarnom uređaju još nije potvrđeno.

## Dodatne provjere nakon izmjena 02/03

- Na Android emulatoru provjeriti Welcome i Permissions na 320 dp širine i u vodoravnom položaju; testirati uz 1.3× i 2× povećanje fonta.
- Na kompaktnom rasporedu potvrditi da su Započni i Nastavi dostupni okomitim pomicanjem bez preklapanja kartica/dozvola.
- Na Androidu 7–9 potvrditi da Kamera i Spremanje pokreću **odvojene** sistemske dijaloge; na Androidu 10+ Spremanje ne smije tražiti zastarjelu dozvolu.
- Stvarna Android UI/emulator i pixel-diff testiranja još nisu izvršena.

## Dodatne provjere nakon izmjene 04 Home

- Na širini 320 dp očekivati jednu karticu po retku, bez odrezanih naziva načina rada; 360 dp i više (do tabletne granice) prikazuju dva stupca.
- Na 600 dp i više ostaje četiri stupca, ali provjeriti razmještaj i čitljivost pri povećanju fonta do 2×.
- Screen reader mora moći aktivirati logotip (O aplikaciji), svaku funkcionalnu karticu i postavke; ikona u kartici ne smije nepotrebno dvostruko izgovarati naziv.
- Stvarne Android snimke zaslona, live CameraX način rada i pixel-diff rezultat nisu potvrđeni.

## 04 Home — provjere nakon dimenzijskog usklađivanja

- Telefon 360dp na 3×: dva stupca, četiri reda kartica minimalne visine 76dp, vertikalni razmak 10dp, banner 146dp i galerija 40dp.
- Tablet 800dp na 2×: četiri stupca, dva reda visine 131dp, banner 218dp, galerija 57dp.
- Potvrditi na uređaju: pune četiri mobilne kartice po vertikali, bez prisilnog preklapanja Gallery akcije, pri povećanom fontu dostupnost svih načina rada pomicanjem.
- Izvorni PNG-ovi nisu screenshotovi stvarno pokrenutog Android UI-ja; ne označavati kao vizualno 1:1.

## Navigacija nakon uklanjanja zaslona Privatnost

- Iz svih Compose ruta i Android back obrade uklonjen je `Screen.PRIVACY`, zaseban zaslon i povratno stanje; zaštite lokacije, EXIF-a i sustavnih dozvola ostaju u odgovarajućim stvarnim funkcijama.
- Otkriven i uklonjen rubni navigacijski problem: zupčanik na `O aplikaciji` i `Licence` otvarao je Postavke, ali je Back pogrešno vodio prema prethodnoj kameri. Novi `shouldRememberSettingsOrigin` osigurava da povratak iz Postavki ide na točan izvor (uključujući Welcome, Permissions, About i Licences), dok povratak iz dubokog `Camera Settings` ne prepisuje izvor. JVM testovi štite ovu logiku.
- U Home ekranu vizualni gumb galerije i dalje ima izvornih 40dp na mobitelu, ali prozirno interaktivno područje sada ima 48dp, a Brendigo simbol minimalno 48dp.
- Ikone testirati na OEM launcherima: Pixel (okrugla maska), Samsung (squircle), Xiaomi, te Android 7 stari PNG; nakon ažuriranja pojedini launcheri trebaju osvježavanje cachea ikona. Sama verzija koda ne dokazuje 1:1 maskiranje bez stvarne snimke početnog zaslona uređaja.
