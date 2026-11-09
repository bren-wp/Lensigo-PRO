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

## Dodatna provjera perspektivne panorame (9. 11. 2026.)

Postojeći `PanoramaRegistration.estimate` koristi lokalno podudaranje značajki i projektivni RANSAC. Pronađen je rubni slučaj: čak i ako je homografija **pojedinačnog para** razumna, ulančane transformacije 3–5 kadrova mogu pomaknuti zadnji kadar izvan unaprijed alociranog raspona ili visine. Prethodni `Canvas` tiho bi odrezao taj kadar i pohranio nepotpunu panoramu. `PanoramaComposer` sada testira sve četiri preslikane koordinate novog kadra i pri izlasku iz platna koristi sigurno prethodno vodeno preklapanje; ako čak i ono ne stane, prekida snimanje i prijavljuje grešku umjesto spremanja oštećene fotografije. `PanoramaOutputBoundsTest` provjerava horizontalno/vertikalno odrezivanje, NaN/Infinity, granične i neispravne dimenzije. Stvarni vizualni rezultat, teksture i paralaksa još zahtijevaju fotografije s različitih uređaja.

## Portret — korekcija segmentacijskih rubova

Postojeći projekt već ima stvarni lokalni ML Kit model za osobe. Referentni portretni obris mogao je odstupati zato što je maska iz rezolucije ~512px na višemegapikselnu fotografiju preslikavana **nearest-neighbor** metodom (blokasti rubovi kose/ruku). Uvedeno je bilinearno očitavanje četiri susjedna uzorka maske, uz eksplicitne provjere NaN/Infinity i fail-safe zaštitu lika. JVM testovi provjeravaju interpolaciju i granične vrijednosti. Ovo poboljšava obradu, ali bez testnih fotografija i Android snimaka **nije dokazana 1:1 podudarnost** s prikazom 09 Portrait.

## 12 Filters — zaustavljanje pozadinskog rada i sigurnost sličica

Pronađena dva rubna slučaja u postojećoj stvarnoj implementaciji live CameraX thumbnailova: (1) `PreviewView.bitmap` može vratiti točno 160px bitmap, kada `Bitmap.createScaledBitmap` vrati **istu instancu**, koju je stari kod odmah reciklirao prije korištenja; (2) kada korisnik brzo mijenja jačinu filtra ili zatvori ekran, neuspjeh/cancel usred generiranja 12 sličica mogao je zadržati prethodno alocirane bitmape bez čišćenja. Sada se identična instanca po potrebi kopira prije oslobađanja izvornika, parcijalni set se čisti pri grešci/cancelu, a kad nema živog PreviewView-a ne vrti se beskoristan periodični posao. Live senzorski kadar i postojeći filteri ostaju stvarne, ne sintetizirane funkcije. Još treba usporediti stvarne screenshotove 12 Filters s izvornim PNG-om.

## HDR Photo — stvarna podrška senzora i izvorni ekran 17

Referenca 17 sada ima **djelotvornu HDR kontrolu** samo kada `ExtensionsManager.isExtensionAvailable(selector, ExtensionMode.HDR)` potvrdi aktivni senzor. Odabrana postavka `hdr_processing` stvarno mijenja CameraX kameru u `ExtensionMode.HDR` pri sljedećem ulazu u način FOTO. Ako ekstenzija ili kombinacija preview/capture nije podržana, koristi se obična kamera bez lažne HDR oznake. Provjera se obnavlja po odabranom prednjem/stražnjem senzoru. Budući da dostupnost ovisi o **stvarno aktivnoj kameri**, UI prije otvaranja FOTO načina prikazuje neprovjerenu dostupnost i ne nudi lažni prekidač. Ovo nije multi-frame softverski HDR na svakom Android uređaju, nego hardverski uvjetovan CameraX HDR. Potvrdu izlaznog kadra i referentne vizualne slike i dalje zahtijeva fizički uređaj.
