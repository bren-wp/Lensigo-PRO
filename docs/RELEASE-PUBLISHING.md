# LENSIGO — APK/AAB i GitHub Releases

## Potvrđeno stanje (9. 11. 2026.)

GitHub Actions run #93 izgradio je APK i AAB, izvršio unit/lint i
package provjere te prenio artefakte, ali je posljednji korak `gh release
create` dobio HTTP 403 (`Resource not accessible by integration`).
To nije pogreška Android kompilacije. `GITHUB_TOKEN` u logu prijavljuje
`Contents: write`, ali GitHub API ipak odbija objavu Releasesa.
Bez dozvole za objavu izdanje se ne smije proglasiti uspješnim.

## Objavljivanje novih izdanja

1. U postavkama repozitorija provjeriti **Settings → Actions → General →
   Workflow permissions** i ograničenja organizacije ili vlasnika.
2. Ako ugrađeni token ostaje blokiran, kreirati *fine-grained personal
   access token* s pristupom **samo** repozitoriju `bren-wp/Lensigo-PRO`
   i dozvolom `Contents: Read and write`. Ne stavljati token u kod,
   issue, commit, README ili javni chat.
3. Dodati token kao Actions secret pod imenom `LENSIGO_RELEASE_TOKEN`
   u **Settings → Secrets and variables → Actions**.
4. Pokrenuti novu radnju **Android Preview Release** na `main`
   (`workflow_dispatch`) i provjeriti da je GitHub Release stvarno
   objavljen sa `app-debug.apk` i `app-release.aab`.

Ako izdavanje dobije odbijenicu 403, workflow označava **upozorenje**
i čuva uspješne APK/AAB u **Actions → workflow run → Artifacts**.
Svaki drugi problem objave nakon pokušaja ponavljanja i dalje prekida job.

## Ispravna oznaka paketa

- `app-debug.apk`: razvojno (debug) potpisan, primjeren za instalaciju/test.
- `app-release.aab`: **nepotpisan** preview AAB, nije spreman za izravno
  slanje u Google Play Console bez produkcijskog upload potpisa.
- Google Play produkcija zahtijeva vlastiti sigurno pohranjen upload
  ključ i odgovarajuće provjere u Play Consoleu.
- Potpuna vizualna podudarnost 18 aktivnih zaslona za mobitel i tablet
  ne smije se tvrditi bez odgovarajućih stvarnih screenshotova.
