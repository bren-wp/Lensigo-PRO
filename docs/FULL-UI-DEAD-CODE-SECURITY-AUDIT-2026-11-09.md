# LENSIGO PRO CAMERA — audit GitHuba, UI ekrana, mrtvog koda i sigurnosti

**Datum:** 9. 11. 2026.  **Repozitorij:** `bren-wp/Lensigo-PRO`  **Grana:** `main`  **Osnova:** izdanje v0.7.60; uključuje novije izravne commitove na `main`.

## Opseg i metodologija

Pregledani su AndroidManifest, Gradle postavke, GitHub Actions, produkcijske Kotlin/Compose datoteke (kamera, media, obrada, editor, navigacija), dostupni JVM testovi i alati za usporedbu piksela. Vizualni izvori su **originalni dostupni referentni PNG-ovi**, kompozitna slika svih 19 mobilnih referenci i originalni tablet Home; nisu dostupne verificirane snimke izvršene aplikacije za sve ekran/profil kombinacije. Stoga **ne postoji valjana brojčana 1:1 ocjena niti odstupanje u postocima**. Sljedeći prioriteti predstavljaju potvrđene razlike u sadržaju, funkciji ili strukturi, ne empirijski postotak razlike između dvaju Android screenshots.

Dovršena je temeljita statička provjera dostupnog koda i konfiguracije. **Nisu** provedeni testiranje na fizičkom Android uređaju, dinamičko fuzz testiranje svih CameraX kamera, penetration test, analiza CVE-a svih tranzitivnih ovisnosti niti stvarni screenshot pixel-diff. Bez toga nije stručno opravdano proglasiti potpuni sigurnosni audit ili 0% odstupanja.

## Audit 19 zaslona

| Br. | Zaslon | Najznačajnije utvrđeno odstupanje prema referenci / stvarno stanje | Prioritet |
|---|---|---|---|
| 01 | Splash | Crtani Compose simbol, valovi, postotno sidrenje logotipa; fontovi i sistemski insets nisu potvrđeni stvarnom snimkom. | Srednji |
| 02 | Onboarding | Relativne visine hero kartice i tipografija ovise o gustoći/visini ekrana. Zupčanik je prethodno otvarao sistemske postavke, sada otvara postavke LENSIGO. | Srednji |
| 03 | Permissions | Realne Android dozvole i opcionalna lokacija bez prikupljanja; tekst i raspored kartica ne mogu se proglasiti pixel-identičnima bez screencapa. | Srednji |
| 04 | Home | Izvorni hero prikaz mnogo je veći od ugrađenog WebP izvora (600×268 px); potencijalno je vidljivo uvećanje/gubitak detalja. Kartice i banner su prethodno zasebno usklađeni za telefon/tablet. | **Visoki** |
| 05 | Photo | Referenca ima fiksno ilustriranu panoramu zalaska sunca i zoom raspored; aplikacija koristi stvarni senzor. Referentno `HDR` ne odgovara univerzalno dostupnom načinu i nije potvrđen pravi HDR pipeline. | Visoki |
| 06 | Video | Referenca prikazuje **4K • 60 FPS** i fiksni brojač. Stvarni Recorder nudi SD/HD prema kameri, stvarni protok vremena i opcionalni zvuk. 4K/60 nije implementiran kao potvrđena mogućnost. | **Kritičan funkcionalni jaz** |
| 07 | Pro | Referenca prikazuje **RAW**; stvarni Pro izvozi JPEG, uz ručni ISO, shutter, WB i MF samo na podržanim Camera2 senzorima. RAW/DNG izvoz nije implementiran. | **Kritičan funkcionalni jaz** |
| 08 | Night | Referentna noćna scena je statički dizajn; stvarni Night koristi opcionalnu OEM ekstenziju ili lokalnu obradu bez jamstva jednakog vizualnog rezultata. | Visoki |
| 09 | Portrait | Referenca prikazuje precizno izdvojen portret; lokalni fallback samo zamućuje središnje ponderirano područje, **ne radi segmentaciju osobe**. | **Visoki** |
| 10 | Panorama | Referenca prikazuje vođeni način spajanja; implementirano je ograničeno spajanje 2–5 kadrova s fiksnim preklapanjem, **bez perspektivne registracije/panorama stitching modela**. | **Visoki** |
| 11 | Dual | Mockup istodobno prikazuje dvije kamere; API koristi istodobno snimanje samo gdje je podržano, inače uzastopni fallback. Prave OEM kombinacije nisu fizički verificirane. | Visoki |
| 12 | Filters | Mockup pokazuje sličice scene; aplikacija generira lokalne sličice iz ugrađenog hero resursa, a ne iz živog senzorskog kadra. Kvaliteta/boje zahtijevaju usporedbu. | Visoki |
| 13 | Gallery | Referenca prikazuje unaprijed popunjenu mrežu primjera; stvarna galerija prikazuje **vlastite stvarne medije**, pa isti sadržaj nije primjeren proizvodnom UI-ju. Boje/margine i grid čekaju screencap. | Srednji |
| 14 | Viewer | Referentni zalazak sunca nije univerzalna slika; stvarni Viewer ima zoom, gestu pomicanja, dijeljenje i brisanje. Potreban mjerni diff trake i alatne ploče. | Srednji |
| 15 | Editor | Referenca ima određenu fotografiju i slider; implementacija ima svjetlinu, kontrast, crop, filtere, rotaciju i lokalni izvoz kopije. Raspored sklopljenih kontrola nije verificiran na uređaju. | Srednji |
| 16 | Settings | **Veliko semantičko odstupanje:** referenca navodi JPEG/HEIF/RAW i 4K/60, ali implementacija ne smije lažno prikazivati te mogućnosti; lokacija u EXIF-u sada je istinito prikazana kao isključena i zaključana, lokalna obrada kao uključena. Zadržana su stvarna mjesta pohrane. | **Visoki** |
| 17 | Camera Settings | Glavni raspored 5 fotografskih i 4 video stavke približen je izvoru. FPS, HDR i stabilizacija nisu hardverski upravljani kao na mockupu; prikazuju se kao status uređaja, a stvarni Night i Audio imaju funkcionalne kontrole. | **Visoki** |
| 18 | Privacy | Koristi izvorni tekst istaknute kartice, grupirane opcije, brending i lokalnu obradu; dimenzije i pozadina čekaju pixel-diff. | Niži–srednji |
| 19 | About | Usklađen izvorni podnaslov, podnožje i logotip; stvarne akcije za web, privatnost i licence. Točan font/razmaci čekaju screenshot. | Niži–srednji |

**Najveći potvrđeni jazovi:** Video 06, Pro 07, Portrait 09, Panorama 10 i Settings 16–17. Oni se ne mogu ukloniti isključivo preslagivanjem Compose elemenata bez implementiranja novih stvarnih kamerinih sposobnosti, a neke ovise o uređaju. **Najveći rizik za izgled:** Home 04 i kamera 05–12 zbog razlike statičke referentne scene i stvarnog sadržaja senzora.

## Sigurnosni audit

| ID | Nalaz | Dokaz / stanje | Status |
|---|---|---|---|
| SEC-01 | Povjerenje u `LENSIGO_*` naziv datoteke omogućuje tuđim medijima ulazak u galeriju na dijeljenoj pohrani. | Na Androidu API 29+ sada se traži `OWNER_PACKAGE_NAME == context.packageName`; bez vlasništva nema prikaza. | **Ispravljeno za API 29+** |
| SEC-02 | Neutralni JPEG prethodno se kopirao, pa odabrana JPEG kvaliteta nije vrijedila i mogli su se naslijediti EXIF metapodaci. | Sada se neutralni i filtrirani JPEG lokalno dekodiraju i ponovno kodiraju; izvorni EXIF ne kopira se. | **Ispravljeno; cijena je moguća promjena rezolucije** |
| SEC-03 | `IS_PENDING` objava nije provjeravala rezultat MediaStore `update`. | Provjera rezultata i uklanjanje nepotpune stavke pri grešci dodani za fotografije, kompozite i kopije. | **Ispravljeno** |
| SEC-04 | Bitmapovi sličica nisu se svugdje oslobađali pri izlazu iz Compose prikaza. | Dodani `DisposableEffect` cleanup za galerijske i kamerine sličice; prethodno i za Editor/Filters. | **Ispravljeno u pregledanim granama** |
| SEC-05 | Suvišne Android dozvole i mreža. | Manifest sadrži CAMERA, RECORD_AUDIO, te WRITE_EXTERNAL_STORAGE uz `maxSdkVersion=28`; nema INTERNET niti location dozvola, `usesCleartextTraffic=false`, `allowBackup=false`. | **Statički prolaz** |
| SEC-06 | Napadna površina izvezenih komponenti. | Manifest eksplicitno izvozi samo launcher `MainActivity`, ne definira vanjske providere. | **Statički prolaz** |
| SEC-07 | Brisanje neodobrenih fotografija. | Android 29 sustavna potvrda ponavlja stvarno brisanje; API 30+ koristi `MediaStore.createDeleteRequest`. Korisnik potvrđuje brisanje. | **Pregledan kod; device test preostao** |
| SEC-08 | Audio privatnost. | Korisnički izbor `record_audio` i zasebna RECORD_AUDIO dozvola zajednički odlučuju hoće li se snimati zvuk. | **Statički prolaz; uređaj preostao** |
| SEC-09 | Android API 24–28 ne podržava `OWNER_PACKAGE_NAME`. | Ondje prepoznavanje po prefiksu ostaje naslijeđeno, pa nije moguće garantirati stvarno vlasništvo. | **Otvoreno ograničenje** |
| SEC-10 | Ovisnosti / binarni release. | Repo koristi AndroidX CameraX, Compose, Core i Kotlin; nisu izvršeni SCA/CVE sken svih tranzitivnih verzija ni reproduktibilni build. AAB je nepotpisan. | **Otvoreno** |
| SEC-11 | Dinamička obrada nedostatka memorije ili prostora, OEM kontrole. | Izvor i JVM testovi pregledani; ne postoje pouzdani fizički testovi svih načina rada. | **Otvoreno** |

Sigurnosna regresijska zaštita: `tools/security_audit.py` i `tools/test_security_audit.py`, automatski uključeni u GitHub Actions. Provjeravaju zabranjene mrežne/lokacijske dozvole, export/backup/cleartext konfiguraciju, vlasništvo nad galerijom i ponovno JPEG kodiranje. Ovo je **kontrola statičkih invarijanti**, a ne zamjena za dinamičko penetracijsko testiranje.

## Full dead-code audit — potvrđeni i potencijalni nalazi

**Potvrđeno uklonjeno:** privatne nepozivane funkcije `PageScaffold` i `SettingToggle` iz MainActivity; duplicirani `MediaLibrary.saveEdited` (ostaje jedina pozivana metoda `MediaActions.saveEdited`); 25 neupotrebljavanih Kotlin import linija u deset datoteka. Zadržani su `CameraControlRules`, pomoćne Compose komponente, Camera2 capability helperi i javne API metode koje se pozivaju iz različitih ekrana.

**Dodatni kandidati za provjeru, ne uklanjati bez dokazivanja:** vektorski `lensigo_icon.xml` i `lensigo_symbol.xml` u `res/drawable` (mogu biti referencirani resursnim ID-evima ili drugim gradlenim alatima), dijelovi dokumentacije i grafičkog prototipa, te potencijalno redundantan local state pri promjeni mode/previewa. Za sigurno uklanjanje resursa treba pregledati i rezultat Android resource shrinkera, manifest, build alat i stvarno paketiranje.

## Provjere koje su još obvezne

1. Za svih **19 mobilnih** i **19 tabletnih** zaslona snimiti Android UI istim viewportom, density i font scale kao izvor; koristiti `tools/pixel_audit_batch.py` za svakog, a područja živog senzora ne predstavljati kao provjerene piksele.
2. Testirati API 24, 28, 29, 30, 34, 35 i 36: spremanje/kopiranje/dijeljenje/brisanje, uskraćene dozvole, punu pohranu, reset i rotaciju.
3. Testirati 1–3 stražnja/prednja CameraX senzora na više OEM uređaja: autofokus/flash/zoom, Pro manual ISO/shutter/WB, Night/Bokeh ekstenzije, stabilizaciju videa i Dual fallback.
4. Izvesti SCA/CVE provjeru ovisnosti, pregled verzija, Android Lint, testove pod stresom i memory profiling na slabijim uređajima.
5. Za Google Play izraditi **potpisan produkcijski AAB** s održivim signing key sustavom, a ne tretirati GitHub debug APK ili unsigned AAB kao produkcijsko izdanje.

**Zaključak:** postoje stvarna funkcionalna i sigurnosna poboljšanja, ali audit ne potvrđuje 0% vizualnih razlika, besprijekoran rad svih OEM funkcija niti potpunu produkcijsku sigurnost. Gornji otvoreni nalazi služe kao mjerljivi kriteriji za sljedeću iteraciju.

## Naknadni popravak snimanja videa

- Ranija opcija `HD` bila je ograničena na CameraX `Quality.HD`, čak i na uređajima koji podržavaju 1080p/4K. Sada odabir „Najbolja” traži kvalitetu redom `UHD → FHD → HD → SD`, a Android CameraX odabire dostupnu. SD ostaje eksplicitna štedljiva opcija; za nove instalacije početni odabir je najbolja kvaliteta, ali postojeća korisnička postavka ostaje očuvana.
- HUD koristi `AUTO` umjesto lažno preciznog „HD” za automatiziran odabir. **To ne jamči 4K niti 60 FPS** — brzina sličica ovisi o kameri, podršci odabrane kvalitete i CameraX implementaciji. Upozorenje o razlici prema mockupu 06 i 16–17 i dalje vrijedi.

## Novi zahvat 09 Portrait — lokalna semantička segmentacija

- Ugrađena je lokalno zapakirana `com.google.mlkit:segmentation-selfie:16.0.0-beta6` (ML Kit; beta API). Nije potreban mrežni pristup tijekom snimanja i app i dalje nema INTERNET permission.
- Prava maska osobe računa se nad skaliranom kopijom (do 512px), a osobni prednji plan čuva se oštar. Zamućuje se samo pozadina, prema jačini Portret kontrole; ispod praga pouzdanosti postoji meki prijelaz.
- Ako segmentacijski model nije dostupan ili obrada pukne, zadržava se stari lokalni umjetnički fallback i snimanje ne mora propasti. Model ne daje garantiranu segmentaciju za sve prizore/osobe; treba testirati uzorak više portreta, posebice kosu i detalje ruba.
- Portretna obrada odvija se samo nakon JPEG capture u pozadinskoj korutini, bez mrežne obrade; korisnički odabrana JPEG kvaliteta ostaje.
