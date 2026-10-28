# Nuvio Universe (unofficial personal fork)

Repository: https://github.com/IkoMosiEntertainmentHub/NuvioDebrifyMobile

Based on NuvioMedia/NuvioMobile's current `cmp-rewrite` branch. Initial base:
`86e0d5bb45f20d541d17f92146d60746420db71d`, 2026-10-28.

## Changes

- The movie detail Collection section becomes Universe, including on TV series.
- Connections are fetched from IMDb: sequels, prequels, spin-offs and remakes.
- Nuvio's existing poster rail, landscape-card preference, TMDB backdrop lookup,
  watched markers, custom posters, unreleased-content preference and title navigation are reused.
- Duplicate titles retain all relationship labels. Up to 50 connection edges are
  requested; this is not a complete franchise graph or chronological watch order.
- Fetching happens separately from the main detail load, with a six-second timeout,
  a retry state and a 64-title, 30-minute public-metadata cache.
- Universe has no Trakt, Simkl, debrid or paid membership dependency. Existing
  optional integrations and TMDB/MDBList behavior remain available.
- Android includes only arm64-v8a libraries. The package name is
  `com.ikomosientertainmenthub.nuvio.universe`; Debug adds `.debug`.
- In-app updates check this fork's Releases, not the upstream app's Releases.
- The saved `COLLECTION` section setting ID is retained for compatibility with
  existing section ordering/visibility. Its display name is Universe.

The implementation is written for this Kotlin project; Debrify's AGPL Dart
application code is not copied into this GPL project. Existing project licensing,
attributions and branding rights still apply. IMDb's endpoint is not established
here as a supported third-party API contract; failure is non-fatal to the app.

## Build and checks

Use the upstream Android toolchain (JDK 17, SDK levels in `gradle/libs.versions.toml`).

```sh
./gradlew :composeApp:testAndroidHostTest --tests 'com.nuvio.app.features.details.UniverseRepositoryTest' -Pnuvio.android.distribution=full -Pnuvio.ios.distribution=appstore
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full -Pnuvio.ios.distribution=appstore
```

The **Universe ARM64** Actions workflow runs these checks, builds an APK, and
asserts the APK contains only `lib/arm64-v8a` native libraries. Download the APK
from its workflow artifacts. Enable Actions in the fork if GitHub disables it.

The optional `TMDB_API_KEY` and `MDBLIST_CLIENT_ID` repository secrets populate
the upstream runtime configuration. A user can also configure their own TMDB key
and MDBList API key in the app. Do not commit credentials or reuse upstream private
OAuth client credentials. No Trakt/Simkl client credentials are required by Universe.

Debug builds are for testing. CI's generated debug signing key is not persistent,
so independently produced debug APKs may require uninstalling the previous debug
app. For install-over updates, use a release build and preserve the same signing key.

To build a signed release using the workflow, configure:

- `UNIVERSE_KEYSTORE_BASE64`
- `UNIVERSE_KEYSTORE_PASSWORD`
- `UNIVERSE_KEY_ALIAS`
- `UNIVERSE_KEY_PASSWORD`

Then dispatch **Universe ARM64** with `release` enabled. Signing keys must remain
private and backed up. Release publication is a separate action; the workflow uploads
an artifact and does not publish a GitHub Release. To use the in-app updater, publish
a signed release APK with a semantic version newer than the installed version.
Increment `CURRENT_PROJECT_VERSION` in `iosApp/Configuration/Version.xcconfig`
for each release so Android version codes advance too.

## Updating from upstream

Start from your clean feature/default branch and run:

```powershell
./scripts/update-universe-upstream.ps1
```

It fetches current upstream `cmp-rewrite` and merges into a new review branch.
It does not reset or force-push. Resolve any conflicts and rerun tests and the
ARM64 build before merging the update. Preserve this fork's application ID,
update repository URL, Universe section and signing key across updates.

## Initial validation

Nine Universe logic tests passed in an isolated JVM harness using the actual
repository service/test sources, with minimal app/platform stubs. They cover
deduplication, relation labels, movie/series conversion, malformed data, TTL and
bounded cache behavior, TMDB ID lookup, retry after failure, timeout, cancellation
and unsupported content. This is not a full Compose/Android build or device test.
The local full Gradle configuration attempt failed before app compilation with
an AccessDeniedException opening a Gradle library JAR. The CI build is intended
to provide the remaining integration and APK validation; inspect its actual result.
