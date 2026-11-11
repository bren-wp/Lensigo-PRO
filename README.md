<p align="center"><img src="branding/lensigo-hero.svg" alt="LENSIGO Pro Camera — službeni vizualni identitet" width="100%"></p>

<h1 align="center">LENSIGO · PRO CAMERA</h1>
<p align="center"><strong>Svaki kadar. Tvoja priča.</strong><br>Every frame. Your story.</p>
<p align="center"><a href="https://brendigo.com">BRENDIGO</a> · Android kamera · Lokalna obrada fotografija</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B-10364C" alt="Android 7.0 i noviji">
  <img src="https://img.shields.io/badge/CameraX-Kotlin-2E83FF" alt="Kotlin i CameraX">
  <img src="https://img.shields.io/badge/Obrada-lokalno-00A8C9" alt="Lokalna obrada">
  <img src="https://img.shields.io/badge/Bez-oglasa-10364C" alt="Bez oglasa">
</p>

## 📷 Kamera koja ti daje kontrolu

**LENSIGO** je Android aplikacija za fotografiranje, snimanje videa i lokalno uređivanje kadrova. Jednostavno tamno sučelje, brzi pristup načinima rada i Brendigo identitet donose fotografiranje bez računa, oglasa, telemetrije i mrežne dozvole u manifestu.

- **📸 Foto:** fokus dodirom, zoom, bljeskalica, omjer 4:3/16:9 i mreža trećina.
- **🎬 Video:** CameraX snimanje uz kvalitetu ovisnu o uređaju, opcionalno snimanje zvuka i pregled snimljenih metapodataka.
- **🎛️ Pro:** ISO, zatvarač, fokus, balans bijele i RAW+DNG samo na kamerama koje podržavaju odgovarajuće kontrole i format.
- **🌙 Noć i portret:** podržane proizvođačke ekstenzije, uz lokalne alternativne efekte.
- **🏞️ Panorama i Dual:** vođeno spajanje kadrova; dvije kamere istodobno samo na kompatibilnim uređajima, inače uzastopno.
- **🎨 Filteri:** 12 efekata i prilagodba intenziteta.
- **🖼️ Galerija i editor:** lokalni pregled, favoriti, dijeljenje, izrezivanje, rotacija i spremanje uređene kopije bez mijenjanja originala.

> Dostupnost HDR-a, RAW-a, UHD-a, 60 FPS-a, ručnih kontrola i istodobnog prikaza kamera ovisi o stvarnom Android uređaju. Dual video nije implementiran. Nije potvrđeno da svaka funkcija radi na svim modelima.

## 🎨 Originalan LENSIGO dizajn

<p align="center"><img src="branding/lensigo-icon.svg" alt="Izvorni LENSIGO znak" width="130"></p>

**Boje:** Graphite `#081827` · Midnight `#10364C` · Cyan `#00D4FF` · Blue `#2E83FF` · White `#F5FAFF`.

Aplikacija koristi originalnu ikonu, prilagodljivu Android ikonu i tamnu sistemsku splash pozadinu. Izvorni rasterski simbol je provjeren prema odobrenoj referenci, ali maskiranje i rubove ikone na Pixel, Samsung i Xiaomi launcherima **još treba testirati na uređajima**.

### 📱 Stvarne slike aplikacije

**Stvarne, uređajem snimljene i provjerene screenshotove još nije moguće objaviti**: nisu pohranjeni u repozitoriju. Namjerno ne prikazujemo dizajnerske koncepte kao stvarne fotografije aplikacije. Nakon snimanja odgovarajućih ekrana galerija će sadržavati stvarne PNG snimke:

| Početna | Kamera | Pro | Galerija | Uređivač | Postavke |
|---|---|---|---|---|---|
| 📱 Za snimiti | 📷 Za snimiti | 🎛️ Za snimiti | 🖼️ Za snimiti | 🎨 Za snimiti | ⚙️ Za snimiti |

**Upute za snimanje i mjerenje svih 36 aktivnih prikaza:** [UI acceptance matrica](docs/UI-ACCEPTANCE-MATRIX.md) · [matrica uređaja](docs/DEVICE-TEST-MATRIX.md).

## 🔐 Privatnost bez kompromisa

LENSIGO obrađuje fotografije lokalno. Manifest nema `INTERNET` dozvolu ni SDK za oglase. Za kameru, mikrofon i starije verzije Androida aplikacija koristi odgovarajuće sistemske dozvole. Samostalna stranica Privatnost uklonjena je; zaštite, kontrola dozvola i lokalna obrada ostaju aktivni.

## ⬇️ Preuzimanje i kompatibilnost

**[Posljednje objavljeno GitHub izdanje](https://github.com/bren-wp/Lensigo-PRO/releases/latest)**

- `app-debug.apk`: testna/debug verzija za ručnu instalaciju.
- `app-release.aab`: trenutačno **nepotpisani** Android App Bundle; nije spreman za izravno objavljivanje na Google Playu.
- Podržani Android: API 24+ (Android 7.0+); `targetSdk=36`, `compileSdk=37`.

GitHub preview izdanje nije zamjena za produkcijski potpisan i testiran build. Upute: [objava izdanja](docs/RELEASE-PUBLISHING.md) · [Google Play provjere](docs/GOOGLE-PLAY-2026.md).

## ⚙️ Izgradnja iz izvornog koda

Projekt koristi Kotlin, Jetpack Compose, CameraX, Camera2 i MediaStore. Generiranje originalne launcher ikone zahtijeva Python.

```bash
python -m pip install Pillow==12.3.0 numpy==2.3.5 CairoSVG==2.8.2
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
gradle :app:bundleRelease
```

Potrebni su Java 17, Android SDK i verzija Gradlea konfigurirana za projekt. Preview verzija aplikacije temelji se na `GITHUB_RUN_NUMBER`; lokalni build koristi vlastiti fallback broj verzije.

## 🧪 Kvaliteta i razvoj

CI izvršava statičke sigurnosne provjere, Kotlin unit testove, Android lint, gradnju APK/AAB paketa i Python provjere. **To još nije dokaz 1:1 pixel-perfect izgleda**: svih 18 aktivnih mobilnih i 18 tabletnih zaslona zahtijeva stvarne screenshotove i usporedbu s izvornim referencama.

Prioriteti prije produkcijske objave: fizičko ispitivanje CameraX funkcija, RAM/baterija/stabilnost, puni sigurnosni i dead-code audit, provjera svih dozvola, potpisani release paket te stvarna galerija snimaka zaslona.

[Detaljna analiza](docs/FULL-UI-DEAD-CODE-SECURITY-AUDIT-2026-11-09.md) · [Mapa zaslona](docs/SCREEN-MATCHING.md) · [UI prihvatni kriteriji](docs/UI-ACCEPTANCE-MATRIX.md)

---

<p align="center"><strong>LENSIGO · PRO CAMERA</strong><br>© 2026 BRENDIGO · <a href="https://brendigo.com">brendigo.com</a><br><small>MIT licenca; ovisnosti zadržavaju vlastite licence.</small></p>
