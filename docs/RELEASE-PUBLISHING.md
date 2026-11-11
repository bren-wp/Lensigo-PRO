# LENSIGO — APK/AAB i GitHub Releases

## Trenutačni postupak

GitHub Actions radi na push na `main` ili na ručni `workflow_dispatch`.
Pipeline gradi APK/AAB, izvršava Android lint i unit testove te provjere
paketa. Potom generira `SHA256SUMS.txt` i tekst izdanja prema stvarnom
commitu, umjesto kopiranja zastarjelih opisa funkcija.

**Objava je obvezan uvjet uspješne isporuke:** ako GitHub Releases API vrati
HTTP 403 ili objava ne uspije nakon ponovljenih pokušaja, workflow završava
pogreškom. Izgrađeni artefakti ostaju u Actions artifacts za dijagnostiku;
to ne znači da je novo izdanje objavljeno.

## Pristup objavi

1. Provjeriti **Settings → Actions → General → Workflow permissions**
   i postavke vlasnika/organizacije.
2. Ako je ugrađeni `GITHUB_TOKEN` blokiran, stvoriti ograničeni fine-grained
   token s `Contents: Read and write` **samo** za Lensigo-PRO.
3. Spremiti ga kao GitHub Actions secret `LENSIGO_RELEASE_TOKEN`. Nikada ga
   ne unositi u README, issue, commit ili log.
4. Pokrenuti `Android Preview Release` na `main`, provjeriti zelene
   build/test/publish korake i otvoriti odgovarajući GitHub Release.

## Integritet preuzetih datoteka

Preuzeti `app-debug.apk`, `app-release.aab` i `SHA256SUMS.txt` u istu
mapu i pokrenuti `sha256sum -c SHA256SUMS.txt`. Hash potvrđuje integritet
preuzetih datoteka; ne potvrđuje autorstvo, reproducibilnost ili sigurnost.

## Važno prije produkcije

- `app-debug.apk` je **debug potpisan** i služi za testiranje.
- `app-release.aab` je **nepotpisan**, pa nije spreman za Google Play.
- Produkcijski AAB i APK moraju imati ispravan potpis/uplink ključ,
  unaprijed provjeren kontinuitet paketa i `versionCode`.
- Provjeriti ažurnost Google Play zahtjeva u trenutku objave.
- 36 aktivnih zaslona (18 mobitel + 18 tablet) treba usporediti sa stvarnim
  snimkama aplikacije; zeleni CI nije dokaz 1:1 izgleda.
