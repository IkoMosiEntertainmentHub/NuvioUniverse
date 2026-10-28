# Universe implementation and builds

Nuvio Universe is a personal Android ARM64 version of Nuvio Mobile, with focused improvements prepared separately for upstream review.

## Detail sections

Official Collections is preserved. Universe is a separate section directly below it, with independent visibility, order and tab grouping. Existing layouts gain the section without changing saved Collections visibility.

The personal version enables Universe by default. The source-only [upstream proposal](https://github.com/NuvioMedia/NuvioMobile/pull/2425) keeps it disabled until enabled in Layout > Details.

## Connections

The independent Kotlin service reads IMDb's website GraphQL endpoint at `https://graphql.imdb.com/`. It requests `follows`, `followed_by`, `spin_off`, `spin_off_from`, `remake_of` and `remade_as`. Reboots are included when IMDb records them under those relationships. References, spoofs and general recommendations are excluded.

This is not a documented supported API contract or a complete curated franchise database. Provider stability and permitted use need review before official adoption. The feature does not require Trakt/Simkl membership.

Full movies, TV movies, TV series and miniseries are accepted. Shorts, episodes, videos and specials are filtered out; known movie runtimes below 40 minutes and series runtimes below 10 minutes are excluded. Unknown runtimes can remain.

Direct results appear first. Expansion checks up to six direct neighbours, with a 30-minute cache bounded to 64 titles and up to 50 connection edges per request. Requests are deduplicated; failures preserve available results and support retry.

## Settings

Sort by oldest air date (default), newest air date, or IMDb vote count. Choose landscape cards (default) or portrait posters independently of the global poster style. Preferences use the existing Nuvio profile settings payload.

## Configuration and builds

Run `python3 scripts/configure-universe.py` before building. It obtains only Nuvio's public anon/publishable client configuration and performs a read-only endpoint check. Private account credentials and service-role keys are never build inputs. TMDB/MDBList can use the existing runtime configuration paths.

The **Universe ARM64** workflow runs tests and builds an optimized release artifact. Without signing secrets, that APK is unsigned for offline signing. It must be signed and verified before publishing as an installable release.

For workflow signing, configure these repository secrets privately:

- `UNIVERSE_KEYSTORE_BASE64`
- `UNIVERSE_KEYSTORE_PASSWORD`
- `UNIVERSE_KEY_ALIAS`
- `UNIVERSE_KEY_PASSWORD`

Then dispatch the workflow with `release` enabled. Preserve the same production key for future updates and keep an offline backup. Never publish the private key or passwords.

## Versions and releases

Personal version names are read from `universe-version.properties`. Build numbers come from official Nuvio's `CURRENT_PROJECT_VERSION` in `iosApp/Configuration/Version.xcconfig`.

Current planned release: **0.6.5-beta-NuvioUniverse-1.0.0 (148)**.

Publish signed ARM64 APKs on [Nuvio Universe Releases](https://github.com/IkoMosiEntertainmentHub/NuvioUniverse/releases), with additions, fixes, installation changes and actual validation results. Earlier ephemeral-key debug APKs have a separate identity from production builds.

## Updating upstream

From a clean checkout, run `./scripts/update-universe-upstream.ps1`. It fetches official `cmp-rewrite` and merges it onto a new review branch. Resolve conflicts and validate before merging into the personal build. The script does not force-push.

Use `upstream-universe-collections` only for the focused official proposal. Keep personal versioning, branding, signing and release-feed changes out of that branch.

## Verified baseline

The personal implementation passed 37 Android host tests and 5 configuration checks, followed by an ARM64 release build. The upstream-only source passed 27 Android host tests and an original-identity debug build. Earlier debug-build device screenshots are in the README and official PR. Current-revision combined-rail screenshots, current account-login verification and iOS verification remain pending.
