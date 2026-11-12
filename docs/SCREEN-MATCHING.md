# LENSIGO — zasloni, referentne dimenzije i status

Izvor: korisnikov paket `LENSIGO_Branding_Android_UI_19_ekrana(1).zip`.

**Referentni mobilni format:** 1080 × 2400 px. **Tablet:** 1600 × 2560 px.
Rekonstrukcija se izrađuje u Jetpack Compose; screenshotovi koncepta nisu stvarne snimke Android aplikacije. Za potvrdu *pixel-perfect* potrebni su screenshot testovi na ciljanim rezolucijama i usporedba boja i rubova. Još nema potvrđene 1:1 podudarnosti. **Aktualno je 17 aktivnih zaslona po profilu uređaja.** Namjenski zasloni `03_permissions` i `18_privacy` uklonjeni su na zahtjev vlasnika; njihovi dizajni su samo arhiva.

| Referenca | Zaslon | Implementacija | Status |
|---|---|---|---|
| 01_splash.png | Splash | Brendirani Compose Splash i simbol LENSIGO | Funkcionalno, nije vizualno 1:1 potvrđeno |
| 02_onboarding.png | Dobrodošlica | Brendirani prikaz i gumb Započni | Funkcionalno, nije 1:1 potvrđeno |
| 03_permissions.png | Dozvole | **Uklonjeno**; samo izvorni Android sistemski dijalozi | Arhivski koncept, ne ulazi u 1:1 audit |
| 04_home.png | Početni izbornik | Responzivne kartice: 8 CameraX načina rada, galerija, postavke, o aplikaciji | Implementirano; 1:1 nije mjereno |
| 05_photo.png | Foto | Preview, 4:3/16:9, bljeskalica, fokus, zoom, okidač | Funkcionalno, 1:1 nije potvrđeno |
| 06_video.png | Video | HD/SD, opcionalni zvuk, recording | Funkcionalno, 1:1 nije potvrđeno |
| 07_pro.png | Pro | ISO/S/EV/WB/MF panel, Camera2 manual kontrola uz otkrivanje podrške, zum | Funkcionalni hardverski uvjetovani klizači; 1:1 nije potvrđeno |
| 08_night.png | Noć | Proizvođački Night preko CameraX Extensions, EV + digitalni fallback | Funkcionalno uz ograničenja; 1:1 nije potvrđeno |
| 09_portrait.png | Portret | Proizvođački Bokeh ili lokalno zamućenje središnje elipse, klizač | Funkcionalno uz ograničenja; 1:1 nije potvrđeno |
| 10_panorama.png | Panorama | 2–5 vođenih snimki, indikator pomaka, završetak/odustajanje, lokalno spajanje | Osnovni način bez automatskog poravnanja; 1:1 nije potvrđeno |
| 11_dual.png | Dual | Istodobni pregled prednje/stražnje kamere uz podršku, sekvencijalni fallback, spajanje fotografija | Osnovni način uz hardverska ograničenja; 1:1 nije potvrđeno |
| 12_filters.png | Filteri | 12 lokalnih LUT/matrix efekata i intenzitet | Funkcionalno za JPEG; preview podrška Android 12+ |
| 13_gallery.png | Galerija | 3 stupca, 4 filtra (Sve/Fotografije/Videozapisi/Favoriti), oznake na snimkama | Implementirano, 1:1 nije potvrđeno |
| 14_viewer.png | Preglednik | Foto/video, Uredi, Podijeli, Spremi kopiju, Izbriši, favorit | Implementirano, 1:1 nije potvrđeno |
| 15_editor.png | Uređivač | Zajednička preview/export obrada crop/rotate/filter, 12 efekata, max 840 px u pretpregledu | Funkcionalno unutar implementiranog opsega, 1:1 nije potvrđeno |
| 16_settings.png | Postavke | JPEG, lokalna obrada, SD video, mreža, timer i opcionalni zvuk | Implementirano, 1:1 nije potvrđeno |
| 17_camera-settings.png | Kamera postavke | Zaseban ekran JPEG, mreža i video kvaliteta | Implementirano djelomično, 1:1 nije potvrđeno |
| 18_privacy.png | Privatnost | **Uklonjeno** na zahtjev vlasnika | Arhivski koncept, ne ulazi u 1:1 audit |
| 19_about.png | O aplikaciji | Simbol i wordmark, naziv, web-stranica, licence, privatnost i stvarna verzija | Implementirano; 1:1 nije potvrđeno |

## Redoslijed dorade

1. Razvojni build i provjera na Android emulatoru + fizičkom telefonu.
2. Foto i Filteri: tipografija, geometrija, stvarni screenshoti, usporedba tolerancije.
3. Video / Galerija / Viewer / Editor.
4. Pro ručne Camera2 kontrole gdje hardver dopušta; softverski fallback.
5. Noćni kompozit, panorama, portretno segmentiranje i dual kamera uz provjeru sposobnosti.
6. Dovršetak Splash/Onboarding, HR/EN lokalizacije i tablet layouta.
7. Secure release signing i produkcijsko testiranje.

## Pošteno označavanje poboljšanja slike

Filtri mogu podesiti kontrast, boje, svjetlinu i lokalno izoštravanje, ali ne mogu vratiti detalje koje senzor nije zabilježio. `lowMemory` ograničava dimenziju slike na 2048 px pri obradi; bez filtra originalna JPEG datoteka sprema se bez recompressiona. `softenNoise` koristi blago lokalno zaglađivanje (nije ML rekonstrukcija).


## Pro / Night / Portrait — vizualna provjera 0.3.0

Iz `izvoz/mobitel/07_pro.png`, `08_night.png` i `09_portrait.png`:
- Referentni status bar i header su tamni, s bijelim oznakama i cijan aktivnim stanjem.
- PRO: 5 kontrola ISO / S / EV / WB / MF ispod pregleda, dok pri dnu ostaju zoom, način rada i bijeli okidač.
- NOĆ: pregled zauzima većinu visine, zoom je uz donji rub fotografije, odabrani NOĆ je cijan.
- PORTRET: vodoravni klizač ZAMUĆENJE preko fotografije, bez glumljenja dubinske maske na uređajima bez Bokeh dodatka.
- Za manje ekrane koriste se Compose dp i proporcionalno raspoređeni elementi; nije obećana 1:1 usporedba dok nisu prikupljeni stvarni screenshotovi na usporedivim uređajima.

**Hardverske funkcije:** RAW nije implementiran; Pro zaslon prikazuje JPEG, a ISO/S/MF se deaktiviraju na uređajima bez ručne Camera2 kontrole. Proizvođačke Night/Bokeh ekstenzije aktiviraju se samo nakon `isExtensionAvailable`. Kad ekstenzija izostane, lokalni Noćni način i portretna eliptična maska ostaju dostupni. Lokalni portret ograničava izlaz na 1600 px da se smanji memorijsko opterećenje.

**Obvezna provjera prije 1:1 oznake:** screenshot tests na 1080×2400 i 1600×2560, uz toleranciju za statusnu/navigacijsku traku, font scaling, kutove, boje, klizače, fiksne visine i stvarni OEM prikaz kamere.

## Panorama / Dual i pokretanje — pregled 0.4.0

- Referenca 10: tamna traka na vrhu, živa kamera, bijeli okvir s porukom za pomicanje udesno i napredak, odabrana PANORAMA oznaka u cijanu, bijeli okidač, povratak u galeriju.
- Referenca 11: velika stražnja kamera i mali portretni prednji pregled (kad je dostupan), tamni donji izbornik, cijan DUAL, bijeli okidač. Na uređajima bez concurrent podrške u malom okviru piše zašto nema prikaza, a zatim se kamera prebacuje za drugu snimku.
- Reference 01–03: namjenski Splash, dobrodošlica i ekrani dozvola. SVG/vector simbol približna je vektorska rekonstrukcija, nije dokazano pixel 1:1 s dostavljenim PNG-om.
- **Nije odrađeno:** snimanje stvarnih screenshotova na Android emulatoru/fizičkom uređaju, diffs/threshold s PNG referencama, simultana ekspozicija oba senzora, perspektivna registracija panorame.
- Responzivnost: klizači i gumbi koriste Compose dp te prilagodljivu veličinu pregleda kamere, ali rasporedi trebaju testove na malim mobitelima, tabletima i velikom fontu.

## Galerija, preglednik, editor i postavke 0.5.0

Referentne slikovne datoteke `13_gallery.png` do `17_camera-settings.png` imaju:
- gornju tamnoplavu traku sa strelicom i centriranim naslovom
- podebljane naslovne blokove s plavim naglascima
- galeriju s tri stupca i četiri vrste sadržaja
- preglednik s donjom trakom od četiri akcije
- editor s velikom fotografijom, pet alata, jednim klizačem i gumbom za spremanje
- postavke s vizualnim grupama; implementirane su samo stavke koje zaista mijenjaju ponašanje kamere

U ovom izdanju postoji odgovarajuća navigacija i funkcionalni alati, ali **još nema potvrde 1:1**. Stvarna kamera i galerija s korisničkim slikama ne mogu se automatski smatrati istom statičnom scenom kao referentni mockup. Potrebne su realne snimke Android emulatora/uređaja za usporedbu koordinata, fontova, paddinga i boja.

## Originalni 512px launch icon — 0.5.x

Ponovno je upotrijebljen originalni Python/CairoSVG generator iz referentnog Brendigo paketa. Izlaz ima identičan RGB sadržaj kao `01_ikona_512x512.png` (512×512, SHA-256 rasterskih piksela `96538ced13ae75393f3feed7a06866a5d9f72f59e35d359c10dc67cca958e2df`). GitHub Action provjerava identitet prije pakiranja. To **ne potvrđuje** vidljivu 1:1 ikonu na svim Android launcherima jer sustav može promijeniti masku, skaliranje i rubove.

## 18 Privatnost / 19 O aplikaciji — implementacija 0.6.0

Uspoređene su korisnikove mobilne reference 1080 × 2400. Dodani su odgovarajući tamnoplavi layout, bijela tipografija, cijan simbol objektiva i grupirane informacije. Na zaslonu privatnosti četiri su reda i informativni panel; na zaslonu O aplikaciji četiri su informacije, aktivni navigacijski redovi i oznaka Brendigo. Za razliku od statičnih mockupova, poveznice otvaraju stvarne stranice ili Android dozvole. Dodana je zasebna stranica licenci.

UI je responzivan kroz Compose `widthIn(max=540.dp)`, okomito pomicanje i `heightIn`, no **vizualno 1:1 nije potvrđeno** bez stvarnih snimki aplikacije na telefonu i tabletu. Izvorna 512×512 launcher slika reproducira se RGB-piksel-po-piksel; Android maskiranje može promijeniti prikaz ikone na početnom zaslonu.

## Nastavak nakon v0.7.21 — Splash i dozvole

- **01 Splash:** standardni raspored i izvorni LENSIGO simbol ostaju nepromijenjeni, a uski i niski prozori dobivaju kompaktniji raspored koji se može pomicati umjesto odrezivanja podnožja.
- **03 Dozvole:** zahtjevi za kameru i pohranu sada su zasebne akcije. Na Androidu 10+ vlastite snimke ne traže zastarjelu dozvolu za pohranu; na Androidu 7–9 ostaje zaseban zahtjev.
- **Izdavanja:** GitHub CI broj izdanja sada je usklađen s Android `versionName` i rastućim `versionCode`, umjesto da svi binarni paketi zadržavaju 0.7.0.
- **Nije potvrđeno:** 1:1 usporedba sa stvarnim 1080×2400 i 1600×2560 Android snimkama, velike pristupačne veličine fonta, ponašanje CameraX na fizičkim uređajima i potpisani Play AAB.

## Nastavak — zasloni 02/03 i pristupačnost

- **02 Dobrodošlica:** na standardnim zaslonima očuvan je postojeći raspored i tipografija. Na niskim/uskim prozorima koristi se manji okvir logotipa i okomito pomicanje radi dostupnog gumba Započni i većeg teksta.
- **03 Dozvole:** mala visina i široko povećanje fonta više ne ovise o elementu koji raste unutar nedovoljno visokog zaslona; sadržaj se pomiče, tako da korisnik može doći do gumba Nastavi.
- **Vizualni status:** nema mjerenja stvarnih snimaka uređaja za 02/03, stoga nije potvrđena 1:1 podudarnost s referentnim PNG-ovima.

## Nastavak — zaslon 04 Početna stranica

- Standardna mobilna referentna mreža od dva stupca i tabletna od četiri ostaje nepromijenjena.
- Na širini manjoj od 340 dp prelazi se na jedan stupac da nazivi načina snimanja ne budu odrezani.
- Kartice dopuštaju dva retka na velikom fontu; zaglavlje i banner prilagođavaju se većim fontovima, a akcije s ikonama imaju pristupačnije oznake.
- **Preostaje:** stvarna usporedba geometrije i boja s 04_home.png, test Android čitača zaslona i instrumentirano testiranje na uređaju.

## Splash reference reconstruction after v0.7.24

From the original `LENSIGO_Branding_Android_UI_19_ekrana(1).zip`:
- Phone source: `izvoz/mobitel/01_splash.png` (1080×2400); tablet: `izvoz/tablet/01_splash.png` (1600×2560).
- The supplied design has a dark `#0E2134` upper background fading to `#08111E`, an aperture starting around 24.5% from the top, four lower cyan/navy arcs and a cyan circular loading indicator.
- This revision models the arcs with Compose Canvas, positions the logo proportionally and hides Android system bars only while the Splash is visible. Regular Android navigation and status bars are restored on later screens.
- **Verification pending:** the above is a source-guided layout reconstruction, NOT an emulator screenshot comparison. OEM system bar transitions, density/font scale and exact pixel scores still need device capture.

## Home reference photography + icon fidelity (original package)

- The source `assets/photo.jpg` (same `LENSIGO_Branding_Android_UI_19_ekrana(1).zip`) supplied the hero artwork for mobile reference 04 and its camera scene. This revision packages a local, center-cropped **300×134 WebP** (about 3 KB) as `res/drawable-nodpi/lensigo_home_hero.webp`. It is not a fabricated landscape or remote image.
- Eight camera-mode icons have changed from filled Material glyphs to outlined variants closer to the 04 reference. Standard card layout (two columns on phone, four on tablet, single narrow-phone fallback) is preserved.
- The smaller WebP is lossy; this is a UI fidelity improvement, not a proof of exact original image pixels. Improve the asset resolution when the full source can be safely checked in, and compare real Android emulator screenshots against 04_home.png before marking 1:1.

## Photo 05 — focus frame and actual thumbnail

- The 78dp preview focus reticle starts centred and follows the real user tap, clamped inside the live CameraX preview while the same tap is sent for metering.
- The bottom-left gallery shortcut displays a bounded, locally decoded last-photograph thumbnail after capture. The image decoding uses Dispatchers.IO; without saved media the shortcut remains an icon, not a mockup.
- The shutter has contextual screen-reader actions for still photo or starting/stopping video.
- Pending: device screenshots and actual OEM autofocus/lens testing. Photo-mode HDR has not been implemented merely to match static reference text.

## References 02 Welcome / 03 Permissions — visual and functional alignment

- Source: `izvoz/mobitel/02_onboarding.png`, `03_permissions.png` and corresponding 1600×2560 tablet PNG files. Standard Welcome cards retain the navy panel, but the aperture is reduced to approximately the reference size (112dp) and page indicators use real filled/outlined circles.
- Welcome/Permissions now show functional back and system settings actions: first-screen back exits, the Permissions back returns to Welcome, and settings opens Android's real app settings rather than an inaccessible or mock dialog.
- The Permissions layout now follows the reference's camera/microphone/media/location sequence. Location is explicitly **not collected or written to EXIF**, rather than promising a permission-dependent feature that the Android app has not implemented. Android 10+ still does not request legacy storage permission.
- **Still unverified:** real screenshots / pixel-diff, large font layout and Android 7–9 permission round-trip. This is reference-driven implementation, not confirmed 1:1.

## 06 Video Camera — recording overlay and ratio

- CameraX `VideoRecordEvent.Status.recordingStats.recordedDurationNanos` now drives the visible red recording timer, which shows hours/minutes/seconds. It appears only during actual recording, never as a static fake 00:00:12 label from the source screenshot.
- First video session starts in 16:9 as reference 06, while still-photo modes start in 4:3. The user can change each ratio independently and both preferences persist locally.
- Source `izvoz/mobitel/06_video.png` and tablet equivalent. The mockup's 4K/60 label is not displayed because only SD/HD capture has been implemented/tested. Device-specific resolution, lens switching while idle, microphone and preview geometry require real physical tests.

## 07 Pro Camera — reference-guided manual control panel

- The supplied `07_pro.png` has five lower dark cards (ISO, S, EV, WB, MF) and a white graduated ruler below. The native Compose overlay now uses 48dp minimum target height for manual controls and renders a 40-tick ruler, with a **cyan indicator linked to the actual selected manual value** (not a decorative fake needle).
- Only genuine Camera2 manual-capability ranges are shown as usable. If the device does not support a control, that control remains disabled and gives honest feedback. The design mockup uses “RAW” and Kelvin values; the actual app currently captures JPEG and exposes available AWB modes, so neither RAW nor a Kelvin slider is falsely advertised.
- Still needs emulator/physical screenshots and pixel-audit comparison on phone/tablet, accessibility reader and hardware capability checks. Added ruler is a fidelity improvement, not proof of a perfect 1:1 match.

## 08 Night — source-driven uncluttered preview

- The original `izvoz/mobitel/08_night.png` and tablet counterpart show the live image without an extra persistent footer label, and a standard zoom control just above the selected mode. The hardcoded footer text has been removed to restore usable preview height.
- The existing real exposure compensation slider remains available on devices supporting EV, but is **collapsed by default** and toggled by tapping the accessible NOĆ header. When the OEM Night extension is active or EV unsupported, no non-functional slider is presented. The actual vendor-extension/local-processing decision remains unchanged.
- Camera sensor content changes on each device; no claim is made that a live preview matches the static reference photograph. Device screenshots and extension fallback tests remain pending.

## 09 Portrait — honest fallback and visual focus guide

- The user reference `izvoz/mobitel/09_portrait.png` includes a large face-area focus frame and a cyan blur slider. Native Portrait mode uses a 210dp focus guide, still responds to actual tap-to-focus and clamps inside the real CameraX preview.
- When CameraX Bokeh extension is available, top header reads BOKEH; when it is not, top header reads PORTRET and the local oval-mask blur slider remains functional. No imaginary device-level background segmentation is claimed, and the extra developer-like footer note has been removed from the view.
- Native screenshot comparison against the 1080×2400 phone and 1600×2560 tablet reference is not yet recorded. Face-specific tracking is not implemented; the rectangle is a visible focus guide, not a face detector.

## 10 Panorama / 11 Dual — framing and hardware capability

- Source `izvoz/mobitel/10_panorama.png`: white translucent central guidance rectangle with the rightward movement instruction, a wide horizontal indicator and a large white arrow. The guidance now follows these elements; the line fill reflects `frameCount / MAX_FRAMES`, not an invented 100% sample progress. Existing Finish/Cancel and 2–5 frame logic remain functional.
- Source `izvoz/mobitel/11_dual.png`: front-view PiP in the upper-right of the main camera image. CameraX's actual concurrent view remains visible only when hardware supports it. Removed redundant footer text during concurrent operation; numbered guidance remains visible for the honest sequential fallback.
- Not verified: panorama perspective alignment, simultaneous still exposure across dual sensors, OEM CameraX camera behavior, or exact 1:1 Android screenshots. Both modes retain existing documented limitations.

## 12 Filters — genuine, source-backed thumbnail strip

- The reference 12_filters.png shows small photographic previews across the bottom of the camera. The old text-only chips are replaced with locally produced photographic previews, using the real source-provided `assets/photo.jpg` optimized for the Home asset. Each preview passes through the **same FilterEngine** and selected intensity as final JPEG processing, avoiding misleading arbitrary color swatches.
- All 12 existing filters remain navigable via a horizontally scrolling strip. The chosen filter is outlined in cyan; previews render off the UI thread with a debounce to limit work while the user drags intensity. No external images, network permissions or cloud processing are introduced.
- Remaining: exact image, font and spacing comparison with original mobile/tablet PNG; actual CameraX preview transformation and low-RAM/long-session profiling are not yet device-verified.

## 13 Gallery / 14 Viewer / 15 Editor navigation

- Gallery keeps three columns of actual LENSIGO MediaStore thumbnails; on narrow widths under 340dp the four category pills become a horizontally scrollable strip instead of truncating names. Other widths retain the reference four-pill row. No developer mockup photographs are inserted in users' galleries.
- Viewer top-right now uses the original reference's horizontal ellipsis menu; Favorite toggle remains functional inside it, alongside Share, Save copy and Delete. Bottom action bar remains accessible.
- Editor's top-right gear now opens actual app settings. Image transformations still use the shared crop/rotate/filter pipeline and save a separate copy rather than overwriting originals.
- Emulator/physical-screen pixel comparisons for all three screens are not yet recorded; real photographs differ from source-designed scene samples.

## 16 Settings / 17 Camera Settings — source-aligned, real controls only

- Reference Settings 16 contains Kamera, Privatnost i pohrana, Aplikacija panels. The real Android menu preserves this main hierarchy. Advanced noise/sharpen/low-memory controls remain **in the actual Camera Settings 17 screen**, rather than appearing in a duplicated extra panel on the main Settings page. The microphone permission remains available in Camera Settings.
- Reference Camera Settings 17 includes an aspect ratio. Added a persisted real 4:3/16:9 still-photo preference, synced with the camera header and retained when switching between Photo and Video. JPEG quality, grid, SD/HD preference and all pre-existing real processing controls still operate.
- The mockup's RAW/HEIF/4K60/HDR toggles were not inserted because the current CameraX capture pipeline has no verified implementation for them. Device-level screenshots and compatibility testing remain required.

## 18 Privacy / 19 About — brand geometry, truthful info, licences

- For mobile reference 18, Privacy headline, cyan LENSIGO aperture and first information card now follow the supplied vertical rhythm more closely. The logo is reduced from 100dp to 64dp, and card rows use 54dp accessible minimum heights; visual group outlines are added.
- Reference 19's central brand block uses a smaller approximately 92dp mark, a 30sp LENSIGO wordmark and adjusted surrounding spacing. The real Android version from `BuildConfig.VERSION_NAME` remains shown instead of the reference mockup's conceptual developer messages.
- The Licences screen now references the actual `github.com/bren-wp/Lensigo-PRO` and opens that URI via a real Android ACTION_VIEW intent. No network permission is added to the camera itself; the user-selected external browser handles opening it.
- These are source-guided corrections. Neither 1080×2400 nor 1600×2560 real emulator screenshots have been compared yet, so 1:1 remains unverified.

## 01 Splash / 04 Home — prilagodba velikim slovima nakon v0.7.34

- Splash 01 pri povećanju fonta ≥1,4× koristi kompaktniji pomični izgled da se logotip, slogan i podnožje ne odrežu; standardni raspored ostaje nepromijenjen.
- Home 04 na tabletima s fontom ≥1,6× prikazuje dvije umjesto četiri kartice u retku, a na mobitelima užim od 420 dp s takvim fontom po jednu; naslovni banner dobiva dodatnu visinu i galerija pristupačan naziv akcije.
- **Provjera 1:1 nije potvrđena:** stvarne Android snimke zaslona i poravnate izvorne PNG reference trebaju se usporediti pixel-audit alatom.

## Funkcionalni i vizualni pregled — dozvole i snimanje (nakon v0.7.39)

- **02 Welcome:** provjera prozora pri fontScale ≥1,35 aktivira pomični raspored; standardni izgled ostaje očuvan.
- **03 Permissions:** uklonjeni su tekstni gumbi koji nisu na izvornim vizualnim referencama. Cijela kartica pokreće samo stvarni Android zahtjev za dozvolu, a čitač zaslona dobiva stanje odobrenja. Na Androidu 10+ lokalne snimke ne traže zastarjelu dozvolu za pohranu.
- **05 Photo / 10 Panorama / 11 Dual:** stvaranje privremene JPEG datoteke sada obrađuje neuspjeh pohrane bez rušenja korisničkog zaslona; neuspjelo stvaranje druge Dual snimke čisti prvu snimku.
- **06 Video:** pri prelasku aktivnosti u ON_STOP zaustavlja se aktivno snimanje i otkazuje samookidač; nakon neuspjeha finalizacije briše se djelomični MediaStore izlaz nastao u toj sesiji.
- **Nije dokazano 1:1:** ove korekcije temelje se na izvornoj referenci i implementacijskom auditu. Provjeru 1080×2400 i 1600×2560 potrebno je provesti na stvarnim Android screenshotovima, kao i fizičke CameraX testove.

## Navigacija i redoslijed kamere (nakon v0.7.40)

- **05 Photo / 06 Video:** traka načinâ snimanja sada u početnom vidljivom području prati izvorni redoslijed `FILTERI / VIDEO / FOTO / PRO / NOĆ`, bez zamjene definicija samih CameraX načina.
- **04 Home / Camera / Gallery / Settings / About:** vraćanje sada poštuje ekran s kojeg je korisnik došao. Camera → Home, Home → izlaz, Gallery → svoj stvarni prethodni zaslon, Settings i About → odgovarajući izvorni zaslon. Svi vidljivi gumbi za povratak usklađeni su sa sustavnom navigacijom.
- Dodani su JUnit testovi da se izvorni redoslijed načinâ rada ne može slučajno pokvariti, a nijedan način rada nestati.
- Potrebni su testovi na uređaju za Back geste, veliki font i CameraX izlaz/povratak; vizualni 1:1 i dalje nije potvrđen stvarnim screenshot usporedbama.

## Filteri, noćna obrada i uređivač — optimizacija memorije (nakon v0.7.41)

- **05 Photo / 08 Night / 09 Portrait / 12 Filters / 15 Editor:** JPEG dekodiranje neće se potpuno prekinuti samo zbog nečitljivog EXIF bloka; u tom se slučaju zadržava izvorna orijentacija umjesto pretpostavljanja nepostojeće rotacije.
- **Lokalni filtri i obrada šuma:** uklonjeno je stvaranje zasebnog `IntArray` objekta za svaki obrađeni redak. Sada se koristi jedan izlazni tampon koji se ponovno upotrebljava. Matematička formula piksela ostaje neizmijenjena.
- **Provjere koje nedostaju:** stvarni memorijski profil na API 24–36 i usporedba izlaznih JPEG piksela na fizičkom uređaju. Nije dokazano da su svi prikazi vizualno 1:1.

## Prioritetni UI/UX pregled 13–19 uz izvorne mobilne/tabletne reference

Izvori: izvorni paket `LENSIGO_Branding_Android_UI_19_ekrana(1).zip` (`izvoz/mobitel/13_gallery.png` do `19_about.png`; mobitel 1080×2400, tablet 1600×2560). Uspoređivani su elementi reference sa stvarnim Compose deklaracijama; ovo nije zamjena za usporedbu screenshotova builda.

- **13 Galerija:** reference imaju tamnoplavu naslovnu traku, naslov „Moji trenutci”, četiri kartice filtara i mrežu od tri stupca. Zadržani su ti elementi; za povećani font omogućen je pomični red filtara. Pogreška čitanja lokalnih medija sada prikazuje izričitu pogrešku i funkcionalni gumb „Pokušaj ponovno”; medijske kartice imaju jasno opisanu akciju otvaranja.
- **14 Viewer:** referenca prikazuje donje `0.6 / 1x / 2x / 5x` gumbe iznad četiri medijske radnje. Prikaz fotografije sada ima **stvarno lokalno zumiranje** odgovarajućim gumbima i dvoprstnom gestom, bez promjene izvornika. Video i uređivanje ne prikazuju lažna foto-zoom stanja. Oštećena slika više ne prikazuje beskonačni indikator bez rezultata.
- **15 Editor:** ekran ima pet alata, pregled fotografije i stvarno spremanje zasebne kopije; Android Back sada prvo zatvara uređivanje umjesto da neočekivano napusti Viewer. Spremanje se ne nudi ako fotografija nije uspješno učitana.
- **16–17 Settings:** cijeli red s prekidačem ima jednu semantičku akciju (nema ugniježđenih prekidača koji mogu izazvati dvostruko prebacivanje). Gumb zupčanika unutar „Postavke kamere” vraća na Postavke, umjesto da neočekivano otvara „O aplikaciji”. Odrezivanje predugih vrijednosti ograničeno je na dva retka.
- **18 Privacy i 19 About:** izvorna grupiranja, boje i raspored zadržani su; interaktivni informacijski retci dobili su eksplicitnu oznaku za čitač zaslona.
- **Preostalo:** fotografirati svih 7 zaslona na uređaju uz identične insets, fontScale i gustoću piksela te provesti stvarni pixel-diff s referentnim PNG-ovima prije bilo kakve tvrdnje o 1:1.


## 05–12 — stvarne kontrole kamere i sigurnosna stanja (iteracija v0.7.44)

- **Bljeskalica:** gumb je vidljivo onemogućen bez stvarnog hardverskog flasha ili prije CameraX bindinga; ne nudi nepostojeće osvjetljenje na prednjoj kameri.
- **Zoom:** prikazuju se samo dostupne razine uz fallback za nestandardnu kameru. Odabrana razina ažurira se nakon što CameraX stvarno prihvati `setZoomRatio`; neuspjeh se javlja korisniku.
- **Omjer, kvaliteta videa, Postavke, galerija, izmjena načina snimanja:** nisu lažno aktivni tijekom snimanja/obrade; promjene omjera spremaju se u postojeće lokalne postavke.
- **Okidač:** gumb je omoguć en tek kad CameraX pripremi stvarni foto/video use case; zaustavljanje snimanja ostaje moguće tijekom aktivnog zapisa.
- **Panorama / Dual / izmjena prednje kamere:** različiti gumbi imaju odgovarajuće pristupačne nazive; pri promjeni objektiva gasi se bljeskalica.
- **CameraX lifecycle:** pri rebindu i izlasku s kamere čiste se reference stare instance, što sprječava pokušaj snimanja putem neaktivnog use casea.
- Dodani su JVM testovi dostupnosti okidača i stvarnih raspona zoom kontrola. OEM CameraX testovi i pravi screenshot pixel-diff ostaju obvezni prije potvrde potpunog 1:1.

## Izravno mjerenje referentne pozadine 13–19 (mobitel 1080×2400)

Dostavljeni izvorni PNG-ovi `13_gallery.png`, `15_editor.png`, `16_settings.png`, `17_camera-settings.png`, `18_privacy.png`, `19_about.png` imaju jednaku postepenu promjenu pozadine (mjereno na x=16): y=100 RGB(14,33,52), y=700 RGB(14,34,53), y=1000 RGB(14,33,52), y=1600 RGB(12,28,45), y=2320 RGB(8,18,31). Prijašnji Compose kod koristio je gotovo jednobojnu tamnoplavu podlogu.

Uvedena je zajednička izvorno izmjerena `LensigoReferenceBackground` gradijentna četverostupanjska pozadina na 13 Galeriji, 15 Editoru, 16–17 Postavkama, 18 Privatnosti i 19 O aplikaciji. Tamna pozadina 14 Viewer ostaje odvojena od Editora kao na referenci. Ovo ispravlja **konkretno potvrđeno odstupanje boja i gradijenta**, ali još nije matematički dokaz pixel-perfect 1:1 jer se izmjerene točke aproksimiraju i nema potvrđenih snimaka stvarne Android aplikacije.

## 01 Splash / 04 Home — izvorni PNG, referentno mjerenje nakon v0.7.46

- Stvarni korisnikov ZIP sadrži 1080×2400 `izvoz/mobitel/01_splash.png` i `04_home.png`, te 1600×2560 odgovarajuće tabletne reference. Izmjerene su izvorne slike, **ne** Android screenshotovi aplikacije.
- **01 Splash:** gornji rub cijan logotipa u PNG-u y≈591/2400 = 24,6% (telefon) i y≈629/2560 = 24,6% (tablet). Postojeći Compose offset 24,5% ostavljen je bez nepotrebnog pomicanja.
- **04 Home mobitel:** referentni retci kartica y=916–1144, 1176–1404, 1436–1664 i 1696–1924, svi visoki 228 px (=76dp na 3 px/dp). Stari minimum 105dp (315px) mogao je sakriti četvrti red ispod ekrana. Novi minimum 76dp i smanjene ikone/padding omogućuju raspored prema PNG-u.
- **04 Home tablet:** referentna prva dva retka približno su y=916–1178 i 1210–1472, tj. 262px (=131dp na profilu 2 px/dp). Novi tabletni minimum 131dp u četiri stupca.
- **04 Home banner:** visina prema izvorniku ≈146dp telefon i ≈218dp tablet; nadnaslov je gore, naslov u sredini i opis pri dnu. Prijašnji jedinstveni 164dp banner i sva tri teksta poravnana pri dnu nisu slijedili izvor.
- **Galerija:** referentni mobilni gumb sada ima 40dp min, tabletni 57dp; akcija je stvarna, ne demonstracijska.
- Pri većem fontu pravila povećavaju dimenzije, čuvaju skrolanje i čitljivost, i zato namjerno ne repliciraju statični PNG.
- **Nema potvrde 1:1 bez realnih Android screenshots i pixel-diff testa**. Kadriranje lokalne WebP naslovne fotografije na visokodenzitetnim ekranima još treba zasebno verificirati.

## 02–03 — potvrda poruka i prelaska u kameru nakon v0.7.47

- Izvorne slike `izvoz/mobitel/02_onboarding.png` i `03_permissions.png` dodatno su uspoređene s Compose kodom. U referenci zaslona 03 donji gumb glasi **„Nastavi na kameru”**. Prije je pisalo samo „Nastavi” i gumb je otvarao Home; sada oznaka i stvarno odredište odgovaraju referenci: Camera.
- Četvrti red u referenci nazvan je **„Lokacija (neobvezno)”**. Zadržana je istinita napomena da aplikacija ne prikuplja lokaciju niti je zapisuje u fotografije; nije dodana lažna geolokacijska mogućnost.
- Uklonjen je dodatni uvodni slogan iznad gumba koji ne postoji na referentnoj slici, a visina gumba je minimalna umjesto fiksne za bolju prilagodbu fontu.
- Početni zaslon Home ostaje dostupan pritiskom na naslov LENSIGO u kameri ili Android Back gestom.
- Ovo je semantička i izvorno-vizualna korekcija. Stvarni screenshotovi pokrenute aplikacije za 1:1 još nisu uspoređeni.

## 05 Foto i 06 Video — vidljivi redoslijed načina rada prema referenci

- `izvoz/mobitel/05_photo.png`: oko aktivnog FOTO vidljive su oznake FILTERI — VIDEO — FOTO — PRO — NOĆ.
- `izvoz/mobitel/06_video.png`: oko aktivnog VIDEO vidljive su DUAL — FILTERI — VIDEO — FOTO — PRO. Prijašnja Compose traka započinjala je FILTERI pa nije mogla prikazati prethodni DUAL u ovom stanju.
- Traka zato sada koristi krajnje duplicirane navigacijske stavke, dok svaki **stvarni CameraMode ostaje jedan jedinstven način rada**. Dodani su JUnit testovi za oba izvorna redoslijeda i svih osam načina.
- U izvornim mockupovima `assets/photo.jpg` već sadrži nacrtanu veliku traku zooma; `assets/video.jpg` već ima fiksni crveni brojač 00:00:12. To su grafički dijelovi *referentnih fotografija*, ne stvarno funkcionirajuće kontrole. Aplikacija namjerno prikazuje jedan stvarni CameraX zoom i stvarno vrijeme snimanja, bez dupliciranja zaostalih elemenata u prikazu kamere.
- Referentne PNG datoteke još nisu uspoređene sa snimkama stvarne aplikacije na emulatoru; 1:1 ostaje neprovjereno.

## 05–12 — kružna vizualna traka načina rada, izvorna usporedba

Provedena je dodatna provjera `05_photo.png`, `06_video.png`, `10_panorama.png` i `11_dual.png` iz originalnog mobilnog ZIP-a. Svaki ekran prikazuje središnji aktivni način s dva lijeva i dva desna susjeda:

- FOTO: FILTERI — VIDEO — **FOTO** — PRO — NOĆ.
- VIDEO: DUAL — FILTERI — **VIDEO** — FOTO — PRO.
- PANORAMA: NOĆ — PORTRET — **PANORAMA** — DUAL — FILTERI.
- DUAL: PORTRET — PANORAMA — **DUAL** — FILTERI — VIDEO.

Traka sada ponavlja samo rubne vizualne stavke radi kružnog prikaza i pamti za PANORAMA/DUAL posljednju pojavu u listi, dok svih osam stvarnih CameraX načina ostaje jedinstveno. JUnit provjere obuhvaćaju četiri izvorna prikaza i dostupnost pet oznaka za svaki od osam načina.

Izvorna pozadinska fotografija `assets/photo.jpg` već sadrži statički nacrtanu veliku zoom traku, a `assets/video.jpg` fiksni mjerač 00:00:12. Aplikacija ne treba duplicirati takve kompozitne dekoracije pri stvarnom snimanju; izvještaj uspoređuje samo stvarne UI slojeve i hardverske kontrole. Stvarna 1:1 usporedba s Android screenshotovima još nije izvršena.

## 07–12 — funkcionalni audit Pro, Night, Portrait, Panorama, Dual i Filters

Provjerene su izvorne reference `izvoz/mobitel/07_pro.png` do `12_filters.png` prema stvarnim Compose i CameraX putanjama (nije izveden fizički test senzora).

- **07 Pro:** vraćanje s ručnog ISO/ekspozicije na EV eksplicitno uključuje automatsku AE kontrolu; povratak s MF na AF eksplicitno vraća kontinuirani autofokus. Kontrole ISO i S aktivne su samo kad Camera2 prijavi odgovarajuću podršku; kod jedne jedine stvarno dostupne vrijednosti zatvarača prikazuje se izvršiva akcija umjesto nefunkcionalnog klizača. Pro ostaje JPEG, bez lažnog RAW obećanja iz mockupa.
- **08 Night / 09 Portrait:** potvrđeno je da se proizvođačka Night/Bokeh ekstenzija koristi samo uz `isExtensionAvailable` i da se u suprotnom primjenjuje lokalna obrada; nije riječ o sintetičkom vraćanju nedostajućih detalja ili pravom ML segmentiranju lica. Offline obrada dijeli istu putanju kodiranja i poziva iz kamere.
- **10 Panorama:** nakon svakog dekodiranog kadra i privremenog skaliranja resursi se oslobađaju u `finally` bloku — i kad kodiranje/skaliranje baci iznimku. Izvorni 2–5 kadrova, progres i vodič ostaju. Spajanje je s fiksnim preklapanjem (ne automatska registracija perspektive).
- **11 Dual:** provjeren je konkurentni par senzora i dvostupanjski fallback. Nije na svim Android uređajima podržano istodobno snimanje dviju kamera, a UI to jasno navodi.
- **12 Filters:** lokalno generirane sličice sada recikliraju nativne bitmap-e pri zamjeni intenziteta ili izlasku s ekrana, umjesto zadržavanja sve većeg skupa filtara u memoriji. 12 filtara i stvarni JPEG izvoz ostaju isti.
- **13–19 ponovna provjera:** galerija prikazuje samo vlastite MediaStore zapise, Viewer zumira fotografije, Editor sprema kopiju bez prepisivanja izvornika, a Settings/Privacy/About imaju stvarno povezane navigacijske akcije. Potrebne su daljnje korekcije prema referenci za kontrolu zvuka videa i izvedive postavke.
- Ne tvrditi 1:1 dok ne postoje Android screencap datoteke i diferencijalna mjerenja za obadva uređaja.

## 06 Video / 17 Postavke kamere / 18 Privatnost — kontrola zvuka snimanja

- U prethodnom kodu `RECORD_AUDIO` dozvola automatski je aktivirala zvuk pri svakom videosnimanju. Izvorna referenca `17_camera-settings.png` prikazuje zaseban **Snimanje zvuka** prekidač.
- Postavljen je lokalni `record_audio` preferencijski ključ (početno uključen samo ako je stvarna dozvola već izdana). Korisnik sada može snimati **video bez zvuka** i kad ima dozvolu za mikrofon, bez potrebe za sustavnim oduzimanjem dozvole.
- Prekidač za video zvuk u Postavkama kamere fizički mijenja `withAudioEnabled()` u CameraX. Kad dozvola nedostaje, pokušaj uključivanja traži izvorno Android dopuštenje. Nijedan audio stream nije uključen ako korisnik onemogući zvuk.
- Tekst Privatnosti sada točno objašnjava izbor bez zvuka i uvjete za mikrofon; nije dodana mrežna komunikacija, analitika ni usluga treće strane.
- Testirana je logika uključivanja zvuka (korisnička postavka × sustavna dozvola) JVM testovima; fizičko slušanje videa, oduzimanje dozvole tijekom snimanja i pauza aplikacije ostaju dio device-test matrice.

## 13–19 — ponovna provjera Viewer, Editor, Gallery, Settings, Privacy, About

Izvor: korisnikov `13_gallery.png` … `19_about.png`, mobilna referenca 1080×2400 i tabletna 1600×2560; povezana stvarna Compose implementacija.

- **14 Viewer:** prethodni pinch-zoom povećavao je fotografiju, ali se nije moglo pomicati po uvećanoj slici. Sada dvoprstna gesta istodobno omogućuje zumiranje i pomicanje, uz ograničenje pomaka na vidljivu regiju; klik na originalne tipke 0.6/1×/2×/5× vraća pomak u središte. Ovo je prava lokalna funkcija, ne dekorativna traka.
- **15 Editor:** privremeno generirane obrađene sličice (promjena svjetline/kontrasta/filtera/cropa/rotacije) sada eksplicitno oslobađaju Android `Bitmap` resurse pri zamjeni ili izlasku iz kompozicije; isto vrijedi za izvornu učitanu sličicu.
- **16–17 Settings:** kartice sada imaju tanki rub kao originalni izvorni PNG; visina redaka prilagođena je izvornoj gustoći umjesto nepotrebnog minimuma od 71dp, a klikabilnost retka/preklopnika ostaje najmanje 56dp i dostupna uz povećani font.
- **18–19 Privacy/About:** visina grupiranih informativnih redaka blago je smanjena radi položaja teksta bližeg referenci, uz min. 50dp za interaktivne poveznice. Privacy opis audio-dozvole sada odražava stvarni funkcionalni prekidač.
- **13 Gallery:** zadržano je samo lokalno čitanje vlastitih medija, tri stupca, četiri filtra i stvarne akcije; kartice i tipografiju potrebno je još provjeriti snimkom izvršene aplikacije.
- Nijedna ovdje navedena korekcija nije automatski potpuna 1:1 potvrda bez stvarnog Android screenshot rezultata.

## 07 Pro — AF/WB sposobnosti senzora, dodatni audit

- Prilikom vraćanja s MF na AF ne smije se automatski slati `CONTROL_AF_MODE_CONTINUOUS_PICTURE`: dostupnost tog režima ovisi o stvarnom objektivu. PRO sada čita `CONTROL_AF_AVAILABLE_MODES`, preferira kontinuirani AF samo kad postoji, zatim obični AF, pa OFF; kada nijedan nije oglašen, ne nadjačava CameraX.
- Balans bijele više ne šalje `AWB_AUTO` kamerama koje takav način ne oglašavaju; bira prvu dostupnu podržanu opciju ili ne postavlja zahtjev.
- Dodani su JUnit testovi za AF fallback na ručno/fiksno fokusiranim objektivima. Na uređajima bez Camera2 manual senzora ISO/S/MF i dalje se ne predstavljaju kao podržane mogućnosti.

## 13 Gallery / 14 Viewer — pregled stvarnog brisanja i pristupačnosti

- `ViewerScreen.delete()`: na Androidu 10 (API 29) prvi poziv može baciti `RecoverableSecurityException` i otvoriti Android sistemsku potvrdu. Prijašnja implementacija nakon odobrenja samo se vraćala u galeriju, bez ponavljanja `contentResolver.delete`. Sada se na API 29 brisanje stvarno ponavlja te se tek nakon uspješnog uklanjanja snimke vraća u galeriju.
- Na API 24–28 `delete()` rezultat provjerava: 0 izbrisanih redaka nije lažno uspješno brisanje. Android 11+ i dalje upotrebljava `MediaStore.createDeleteRequest` koji briše potvrđene snimke kroz sustav.
- Galerijske kartice filtara sada izgovaraju je li pojedina kartica **odabrana** radi TalkBack pristupačnosti, umjesto da odabir bude vidljiv isključivo kroz cijan boju.
- Na stvarnim API 24/29/30/36 uređajima ostaje obvezno provjeriti brisanje, povratak u galeriju i osvježavanje popisa; kompilacija i unit testovi ne zamjenjuju taj test.

## Vizualni i funkcionalni popravak zaslona 17 — izvorna referenca

- Ponovno otvoren original `izvoz/mobitel/17_camera-settings.png` (1080×2400), referenca za **05–12** nije zamijenjena opisnom pretpostavkom. Izvorni gornji ekran ima pet stavki pod FOTOGRAFIJA: Razlučivost, Format fotografije, HDR obrada, Omjer slike, Noćna obrada; te četiri pod VIDEO: Razlučivost, Broj sličica, Stabilizacija, Snimanje zvuka.
- Prijašnji Compose UI je imao JPEG klizač i lokalne filtre izravno među glavnim fotografskim stavkama, što je veliki rasporedni i hijerarhijski odmak od slike. **Početni prikaz sada ima jednak broj i slijed stavki.**
- Stvarno djelotvorne opcije: omjer 4:3/16:9, stvarni postojeći odabir SD/HD za CameraX, `night_processing` koji uključuje/isključuje Night vendor ekstenziju i lokalno poboljšanje, te korisnička kontrola zvuka videa.
- Vrijednosti bez izravne kontrole CameraX-a prikazuju se **isključivo informativno**: JPEG (RAW/HEIF nije implementiran), HDR i broj sličica koje odabire uređaj, stabilizacija prema uređaju. Nije lažno napisano 4K, 60 FPS ni omogućen prekidač za stabilizaciju koja ne upravlja stvarnim hardverom.
- Postojeće i stvarno funkcionalne dodatne kontrole (JPEG kvaliteta, mreža kadra, štedljiva obrada, smanjenje šuma, izoštravanje) ostaju dostupne pritiskom zupčanika u zaglavlju, unutar zasebnog naprednog prikaza. Zadana referentna kompozicija ostaje netaknuta.
- Potpuni pixel-diff nije moguć samo iz izvornog PNG-a: za provjeru 1:1 treba stvarni `screencap` pokrenute Android aplikacije pri odgovarajućoj gustoći i urezima zaslona.

## 18 Privacy i 19 About — točan sadržaj izvornog PNG-a i proporcije

- Na korisnikovom `18_privacy.png` istaknuta kartica glasi **„Vizualni koncept sigurnosnih opcija.”**, a ne **„Obrada bez poslužitelja.”** kako je bilo u aplikaciji. Sada su prikazani izvorni naslov, izvorna objašnjavajuća rečenica i izmjereni rub kartice.
- To ne znači da je sigurnost dokazano testirana: izvorni tekst izričito kaže da stvarna svojstva potvrđuju Android testovi. Korisnik i dalje ima funkcionalan red za sustavne dozvole, a detalji o mikrofonu ostaju u Postavkama kamere.
- Izvorni logo na `18_privacy.png` u mobilnoj referenci zauzima oko 50dp, dok je u prethodnom Compose kodu zauzimao 64dp; na mobitelu sada koristi 52dp, a na tabletu zadržava veći vizual.
- Na `19_about.png` originalni podnaslov je **„Koncept Android dizajna”**, a podnožje **„© 2026 Brendigo · Vizualni koncept”**. Vraćeni su točni tekstovi, smanjen mobilni logotip i glavni naslov, a tabletna prilagodba ostaje povećana.
- U skupu promjena nisu generirane nove slike; koriste se izvorni Brendigo simboli. Potvrda 1:1 zahtijeva stvarni screenshot aplikacije.

## 05–12 — ispravljena stvarna centriranost aktivnog načina

- Prethodni `animateScrollToItem(index - 2)` pokazivao je dvije prethodne kartice, ali tekst aktivnog načina rada nije bio nužno **točno na sredini** zaslona: nazivi imaju različite širine (NOĆ, PANORAMA, FILTERI), a i zasloni se razlikuju.
- Novi algoritam prvo prikazuje izvorni kružni susjedni redoslijed, zatim očitava stvarni položaj/širinu vidljivog aktivnog `LazyRow` elementa i pomiče traku točno za izračunati centrirajući broj piksela. Ne koristi simulirane jednake širine teksta.
- JUnit testovi pokrivaju nultu, negativnu i pozitivnu korekciju položaja, uključujući referentne insets; postojeći testovi i dalje osiguravaju svih osam načina.
- Potvrda pixel-perfect 1:1 cijelih zaslona 05–12 zahtijeva snimke živog emulatora s usklađenim kadrom i maskiranim samo promjenjivim područjima senzora.

## 16 Settings / 17 Camera Settings — mjerenje kartičnih redaka (mobilni raster)

- U izvornoj mobilnoj referenci 1080×2400 opći zaslon Postavke ima 5+3+3 informacijska retka unutar tri grupirane kartice. Prethodni Compose minimum od 56dp uz dodatno vertikalno odstupanje zauzimao je previše visine i potiskivao najdonju karticu ispod vidljive zone.
- Postavljena je minimalna visina reda od 48dp (Android preporučena dodirna površina), uz smanjene unutarnje vertikalne razmake; prilagodljivi tekst i dalje smije povećati karticu kad je uvećan sistemski font.
- S uključenom 17 Camera Settings izvorno usklađenom hijerarhijom 5+4 retka ova korekcija omogućuje bolju vidljivost svih devet stavki bez preklapanja. Dodatni JPEG/postprocessing alati ostaju u prikazu naprednih postavki zupčanika.
- Uklonjen je neiskorišteni `active` lokalni izraz u `SettingsItem` (mali dead-code cleanup); klik/prekidač i TalkBack ostaju ista jedna semantička akcija.
- Bez Android screencap provjere nije moguće potvrditi točnu pikselnu podudarnost izvornih PNG-ova.

## Aktualna navigacija i dozvole

Na prvom pokretanju: Splash → Welcome → Kamera. Na kasnijim pokretanjima: Splash → Kamera. Nema dodatnog zaslona Dopuštenja. Kamera se odobrava u Androidovu izvornom dijalogu, a dodatna prava traže se samo kada korisnik pokrene značajku koja ih zahtijeva. Dijelovi iznad koji citiraju starija izdanja povijesni su, ne opis aktualne verzije.
