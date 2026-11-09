<p align="center"><img src="branding/lensigo-hero.svg" alt="LENSIGO — Pro Camera by Brendigo" width="100%"></p>
<h1 align="center">LENSIGO · PRO CAMERA</h1>
<p align="center"><strong>Svaki kadar. Tvoja priča.</strong><br>Every frame. Your story.<br><a href="https://brendigo.com">BY BRENDIGO</a></p>

<p align="center"><img alt="Android 7+" src="https://img.shields.io/badge/Android-7.0%2B-00C8F5?style=for-the-badge"><img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-CameraX-2E83FF?style=for-the-badge"><img alt="Privacy" src="https://img.shields.io/badge/Privacy-Offline-10364C?style=for-the-badge"><img alt="License" src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge"></p>

## 📷 Profesionalniji kadar — čak i na skromnijem telefonu

LENSIGO je Brendigo Android kamera s jednostavnim sučeljem, lokalnim efektima i postavkama optimiziranima za slabije uređaje. Nova verzija donosi vođenu panoramu i Dual snimanje s provjerom stvarne podrške hardvera. Bez računa, oglasa, telemetrije ili internetske dozvole.

### ✨ Stvarne mogućnosti u verziji 0.7.0

| 📸 Foto | 🎞️ Video | 🎨 Filteri | 💡 Poboljšanja |
|---|---|---|---|
| Fokus dodirom, omjer 4:3/16:9, zum, bljeskalica | Snimanje SD/HD prema dostupnosti uređaja; mikrofon samo uz dozvolu | 12 lokalnih efekata s regulacijom intenziteta | EV kompenzacija, posvjetljivanje noćnih snimaka, blago zaglađivanje šuma, izoštravanje |
| 📁 Galerija | 🪄 Uređivanje | 📱 Kompatibilnost | 🔐 Privatnost |
| Snimke kreirane aplikacijom | Svjetlina, kontrast, zasićenost, filtri i spremanje nove kopije | Android 7.0+, način ograničene memorije | MediaStore, lokalna obrada, bez cloud servisa |

**Pro 0.7.0:** Ručni ISO, brzina zatvarača, WB i MF putem Camera2 podržani su samo kad hardver izloži odgovarajuće karakteristike; u suprotnom kontrole su onemogućene, uz EV kompenzaciju kada je dostupna. **Noć 0.7.0:** proizvođački Night dodatak kada je dostupan, inače lokalno posvjetljivanje. **Portret 0.7.0:** proizvođački Bokeh kada je dostupan, inače umjetničko zamućenje izvan središta kadra koje ne prepoznaje obris osobe.

**Panorama 0.7.0:** 2–5 kadrova s vođenim pomicanjem udesno, lokalno spajanje s 28% preklapanja i spremanje JPEG-a. Jednostavno je ručno poravnanje, bez računalnog prepoznavanja značajki; treba mirno pomicati uređaj. Rezolucija i broj kadrova ograničeni su radi rada na slabijim telefonima.

**Dual 0.7.0:** na telefonima s podrškom CameraX concurrent camera moguć je istodobni pregled prednje kamere u malom okviru iznad stražnje; dvije se fotografije snime redom i lokalno spoje u jednu. Na ostalim telefonima aplikacija jasno vodi kroz dva uzastopna kadra. Nema lažnog prikaza druge kamere. Dual video nije implementiran.

**Pokretanje:** brendirani Splash → dobrodošlica → dozvole (samo prvo pokretanje) → početni zaslon; kasnije se kamera otvara odmah.

### 📁 Galerija / Viewer / Editor 0.7.0

Galerija s četiri aktivna filtra (**Sve, Fotografije, Videozapisi, Favoriti**) i 3-stupačnim prikazom snimaka. Preglednik s dijeljenjem, dupliciranjem u MediaStore, fav oznakom i potvrdom brisanja putem Androidovog sustava dozvola. Uređivač podržava **svjetlinu, kontrast, 12 filtara, intenzitet, središnje izrezivanje na 1:1 / 4:3 / 3:2 / 9:16 i rotaciju od 90°**. Spremanje uvijek stvara novu kopiju — original ostaje neizmijenjen. Način obrade s manjom potrošnjom RAM-a ostaje dostupan.

### ⚙️ Kamera i postavke 0.7.0

Mreža trećina preko live previewa, samookidač 3/5/10 s i opcionalni zvuk okidača. JPEG kvaliteta (70–100%), SD/HD snimanje, pojačavanje oštrine i lokalno ublažavanje šuma. Implementirane postavke su stvarne; RAW/DNG, HEIF, EXIF lokacija, 4K/60 i potpuna HR/EN lokalizacija **još nisu podržani** i ne označavaju se kao funkcionalne.

**12 efekata:** Original · Vivid · Film · Mono · Warm · Cool · Vintage · Fade · Dramatic · Sepia · Noir · Sunset.

> Nema čarobnog povećanja stvarne rezolucije: filtri poboljšavaju izgled postojećih fotografija, ali ne mogu stvoriti detalje koje senzor nije snimio. Za Android 12+ predviđen je grafički pregled filtera na kameri; na starijim verzijama učinak se vidi na spremljenoj snimci i u editoru.

## 🎨 Originalni Brendigo identitet

<p align="center"><img src="branding/lensigo-icon.svg" alt="LENSIGO aperture" width="150"></p>

**Launch ikona — izvorni pikseli:** GitHub Actions prije Gradle builda pokreće `tools/generate_launcher.py` i generira originalni 512 × 512 PNG u `mipmap-nodpi/lensigo_launcher.png`. Izlaz je lokalno uspoređen s korisnikovom referencom i daje **identična 262.144 RGB piksela** (SHA-256 `96538ced13ae75393f3feed7a06866a5d9f72f59e35d359c10dc67cca958e2df`). Gradle build pada ako SHA nije točan. To potvrđuje izvorni sadržaj ikone, ne izgled Androidova maskiranja na različitim launcherima.

**Graphite** `#081827` · **Midnight** `#10364C` · **Cyan** `#00D4FF` · **Blue** `#2E83FF` · **White** `#F5FAFF`.

Svi zasloni koriste tamnu navigaciju i lokalni prikaz kamere. Galerija i uređivač u verziji 0.7.0 bliži su referencama, ali screenshot testovi tek trebaju potvrditi raspored i dimenzije. Za 1:1 provjeru svake referentne slike: [detaljna tablica implementacije i statusa](docs/SCREEN-MATCHING.md).

Originalni korisnički referentni paket sadrži 19 mobilnih i 19 tabletnih konceptnih PNG-ova. Koncepti **nisu** predstavljeni kao stvarni screenshotovi aplikacije; stvarni se screenshotovi dodaju nakon testova.

## ⚙️ Tehnička arhitektura

- Kotlin + Jetpack Compose + AndroidX CameraX; MediaStore i Camera2 kompatibilne kontrole.
- Uređivanje u pozadini, bez uploadanja slika; JPEG obrada ograničena na 2048–3072 px zbog memorije.
- Kamera bira najvišu podržanu UHD/FHD/HD/SD razlučivost; SD je dostupna štedljiva opcija. Kombinacija 4K/60 FPS ovisi o senzoru i snimatelju.
- Razdvojena jezgra: `FilterEngine.kt`, `MediaLibrary.kt`, `GalleryScreen.kt`, `MainActivity.kt`.
- **Package** `com.brendigo.lensigo`, minSdk 24, targetSdk 36.

```shell
python -m pip install Pillow==12.3.0 numpy==2.3.5 CairoSVG==2.8.2
gradle :app:assembleDebug
gradle :app:bundleRelease
```

Gradle automatski regenerira `lensigo_launcher.png` prije izgradnje na lokalnom stroju (potrebni su Python 3 i navedene biblioteke). Sama ikona neće biti pohranjena u repozitorij kao binarna datoteka, ali se deterministički uključuje u APK i AAB. CI također provjerava SHA-256 piksela.

GitHub Actions nakon **uspješnog** builda objavljuje novi preview Release s `app-debug.apk` (testni APK) i `app-release.aab` (nepotpisani bundle). Za produkcijsku objavu potreban je zaseban sigurni signing workflow i testiranje stvarnih uređaja.

### 🧭 Trenutačne mogućnosti i otvorena ispitivanja

Pro način koristi stvarni RAW+DNG na podržanim senzorima, Portrait koristi lokalnu segmentaciju osobe, a Panorama sadrži perspektivno podudaranje kadrova uz sigurni fallback. Potrebni su testovi tih funkcija na različitim Android senzorima, daljnji foto-alati, provjera istodobnog Dual videa i dokumentiranje stvarne pixel-diff usporedbe svih preostalih zaslona. Funkcije ovisne o uređaju nisu predstavljene kao univerzalno dostupne.

### 🔐 Privatnost i brending · 0.7.0

**Zasebni zaslon Privatnost je uklonjen:** privatnosne zaštite ostaju u implementaciji, a stanje pohrane i EXIF lokacije vidljivo je u Postavkama. Korisnik upravlja Android dozvolama putem sustava ili zahtjeva za dopuštenje u trenutku korištenja funkcije.

**O aplikaciji:** brendirani LENSIGO wordmark, stvarna verzija iz `BuildConfig.VERSION_NAME`, prečac do `brendigo.com` i stranica licenci. Poveznica na web otvara se vanjskim preglednikom, bez ugrađene mrežne komunikacije kamere.

**Stabilnost:** Samookidač se prekida pri odlasku sa zaslona ili promjeni načina snimanja. Dozvole se ponovno očitavaju po povratku iz postavki Androida. Galerija provjerava doslovan prefiks `LENSIGO_` i lokalni MediaStore direktorij na novijim Androidima; stariji Androidi koriste kompatibilni fallback. Dekodiranje sličica na starijim uređajima ograničeno je na manju veličinu slike.

**Testovi:** GitHub Actions pokreće Gradle debug/release build, JUnit testove, Android lint te Python testove izvještavanja o pikselnim razlikama. *CI ne zamjenjuje testiranje fizičkih kamera niti dokaz 1:1 identičnosti svih zaslona.*

### 🔒 Privatnost i licenca

© 2026 Brendigo — https://brendigo.com · MIT LICENSE. CameraX i druge AndroidX komponente imaju vlastite open-source licencne obveze.

### 📋 Matrica uređaja i 1:1 provjera

[Popis testova Android uređaja, svih 19 mobilnih i 19 tabletnih ekrana](docs/DEVICE-TEST-MATRIX.md). Vizualne razlike mjere se uz usklađenu rezoluciju i izvorni sadržaj scene. Screenshotovi korisničkih dizajnerskih koncepata nisu predstavljeni kao stvarne snimke aplikacije.

## 🧩 Novosti 0.7.0 — Google Play i UI

- **Početna stranica 04:** responzivna mreža od 8 stvarno povezanih CameraX načina rada, originalan Brendigo identitet te izravne poveznice za galeriju i postavke. Mobilni i tabletni izgled trebaju potvrditi screenshot testovi.
- **Uređivač 15:** jednaki slijed crop → rotate → filter za pretpregled i spremanje; pretpregled max. 840 px radi slabijih uređaja. Obrada je lokalna i sprema novu fotografiju. Zrcaljeni EXIF kadrovi obrađuju se prilikom dekodiranja.
- **Galerija 13:** ograničen izvoz video-sličica radi memorije na Android 7–9.
- **Play 2026:** `targetSdk=36` (Android 16), `compileSdk=37`, `minSdk=24`; Android Gradle Plugin 9.3.1 uz Gradle 9.6, Kotlin Compose plugin 2.4.21. Stable Compose BOM 2026.09.00, Activity 1.13.0, Lifecycle 2.11.0, Core 1.19.1, ExifInterface 1.4.2, CameraX 1.6.2.
- **CI:** APK/AAB, JUnit, Android lint za debug i release, provjera native knjižnica i paketa, Python pixel audit; svaki neuspjeh zaustavlja objavu izdanja.
- **Važno:** ni Build Success ni sličnost UI koda ne potvrđuju svaki zaslon pixel-po-pixel 1:1. Za Google Play potrebni su potpisani AAB, stvarno testiranje, Play Console izjave i pregled pravila.

[Usklađenost s Google Playem](docs/GOOGLE-PLAY-2026.md) · [Pixel audit za sve zaslone](docs/PIXEL-AUDIT.md) · [Matrica uređaja](docs/DEVICE-TEST-MATRIX.md)


### Adaptive launch ikona (Android 8+)

LENSIGO sada koristi pravi **adaptive icon** (`mipmap-anydpi-v26/lensigo_launcher.xml`) za kružne, squircle i druge OEM maske: tamna `#071120` pozadina pokriva cijelu masku, a **originalni odobreni simbol** iz `tools/generate_launcher.py` izrađuje se kao proziran 512px foreground unutar Androidove središnje sigurne zone. Time se uklanja sistemski bijeli okvir koji neki launcheri dodaju kada je dostupan samo starinski PNG. Za Android 7 ostaje originalni checksumom verificirani 512px `mipmap-nodpi` PNG. Proporcije logotipa u stvarnom sustavskom launcheru i dalje ovise o OEM maski i trebaju provjeru na Pixel, Samsung, Xiaomi i drugim uređajima.

Zasebni ekran **Privatnost** uklonjen je na zahtjev vlasnika aplikacije. Android dozvole, lokalno spremanje i stvarne zaštite privatnosti **nisu uklonjene**; osnovni podaci ostaju unutar odgovarajućih postavki. Zaslon O aplikaciji prikazuje stvarnu verziju aplikacije umjesto zastarjelog dizajnerskog teksta.
