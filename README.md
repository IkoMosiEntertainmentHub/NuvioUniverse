<div align="center">

# Nuvio Universe

**Personal Nuvio Mobile builds · Universe discovery · Upstream contributions**

Explore connected movies and series while keeping the Nuvio experience you already use.

[Releases](https://github.com/IkoMosiEntertainmentHub/NuvioUniverse/releases) · [Development builds](https://github.com/IkoMosiEntertainmentHub/NuvioUniverse/actions/workflows/universe-arm64.yml) · [Upstream contribution](https://github.com/NuvioMedia/NuvioMobile/pull/2425) · [Official Nuvio](https://github.com/NuvioMedia/NuvioMobile)

</div>

## About this repository

**Nuvio Universe** is IkoMosiEntertainmentHub's personal version of Nuvio Mobile and a workspace for developing improvements that can be proposed to the official project.

It is based on [Nuvio Mobile](https://github.com/NuvioMedia/NuvioMobile), built with Kotlin and Compose Multiplatform. The original project and its contributors retain credit for the app. This repository provides personal Android ARM64 builds; proposed upstream changes are kept on a separate, focused branch.

## What Universe adds

- **Collections stays:** the official collection rail remains intact, with Universe directly below it.
- **More franchise relationships:** prequels, sequels, spin-offs, follows/followed-by, remakes, and reboots recorded by IMDb.
- **Full movies and series:** shorts, individual episodes, brief videos and known short runtimes are filtered out.
- **Your preferred order:** aired oldest to newest (default), newest to oldest, or most popular by IMDb votes.
- **Your preferred cards:** landscape by default, with an independent portrait-poster option.
- **Earlier loading:** cached results and available artwork appear while the remaining connections load.
- **Existing Nuvio accounts:** retains guest mode and the normal account/profile settings system. Universe preferences use the existing profile-sync payload.

Find its controls under **Settings → Layout → Details → Universe**. Universe itself does not require a Trakt or Simkl membership. IMDb connection coverage varies by title.

## Preview

Owner-supplied screenshots from an earlier working debug build. They show Universe and its portrait/landscape cards; they predate the latest restoration of Collections above Universe.

| Reacher — portrait | Jack Reacher — portrait | Reacher — landscape |
| --- | --- | --- |
| <img width="250" alt="Reacher Universe in an earlier debug build" src="https://github.com/user-attachments/assets/dc1a7d8f-fee6-436d-903d-efea375d0805" /> | <img width="250" alt="Jack Reacher Universe in an earlier debug build" src="https://github.com/user-attachments/assets/a6fd907d-f64c-48a7-b791-94a80d1df3d4" /> | <img width="250" alt="Reacher Universe landscape cards in an earlier debug build" src="https://github.com/user-attachments/assets/f2cce752-d33d-449a-afce-4e702c3efe1e" /> |

## Downloads and versioning

**Status: preview. The first signed release is being prepared.** Published, installable APKs will be listed on [this repository's Releases page](https://github.com/IkoMosiEntertainmentHub/NuvioUniverse/releases). Development artifacts are available through Actions; unsigned release artifacts need signing before installation.

Personal builds target **Android arm64-v8a** and use a separate app identity from official Nuvio. Earlier debug builds also have a separate identity.

Version format:

```text
<official version>-NuvioUniverse-<personal version> (official build number)
0.6.5-beta-NuvioUniverse-1.0.0 (148)
```

The number in parentheses comes directly from the official Nuvio version configuration. The personal version tracks changes made here. Release notes describe additions, fixes, installation changes and validation.

## Contributing to official Nuvio

[**PR #2425 — feat(details): add Universe below Collections**](https://github.com/NuvioMedia/NuvioMobile/pull/2425) proposes the feature to the official project and links the existing [feature request #1789](https://github.com/NuvioMedia/NuvioMobile/issues/1789).

The upstream proposal preserves official branding, accounts, versions, architectures and update feeds. It makes Universe opt-in; this personal build enables Universe by default. Maintainer review and acceptance are separate from publishing a personal build.

| Branch | Purpose |
| --- | --- |
| `cmp-rewrite` | Main source for this personal version |
| `upstream-universe-collections` | Focused Universe implementation proposed upstream |
| `universe-source-validation` | CI validation of the upstream proposal |
| `universe-arm64` | Personal-build development history |

GitHub's fork relationship is retained so source changes can be submitted upstream.

## Validation

- [Personal build](https://github.com/IkoMosiEntertainmentHub/NuvioUniverse/actions/runs/40659125954): **37 Android host tests**, **5 configuration checks**, and a successful ARM64 release build.
- [Upstream source proposal](https://github.com/IkoMosiEntertainmentHub/NuvioUniverse/actions/runs/40659773634): **27 Android host tests** and a successful original-identity Android app build.
- Both downloaded test reports had zero failures/skips and matched GitHub's artifact checksums.

The screenshots document earlier real-device use. Final combined Collections/Universe device screenshots, current account-login verification and iOS validation are still pending.

## Build from source

Requires **JDK 17**, the **Android SDK**, and **Python 3**.

```bash
git clone --branch cmp-rewrite https://github.com/IkoMosiEntertainmentHub/NuvioUniverse.git
cd NuvioUniverse
python3 scripts/configure-universe.py
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full -Pnuvio.ios.distribution=appstore
```

On Windows, use `python` and `.\gradlew.bat` if those are your installed command names.

The configuration script uses Nuvio's public account-discovery document and validates the endpoint without signing into a user's account. It never needs your account password. See [UNIVERSE.md](UNIVERSE.md) for provider details, signing, tests and upstream updates, and [CONTRIBUTING.md](CONTRIBUTING.md) for changes to this repository.

## Credits and license

Based on **Nuvio Mobile** by [NuvioMedia and contributors](https://github.com/NuvioMedia/NuvioMobile/graphs/contributors). Universe's behavior was inspired by the request in #1789 and Debrify; the Kotlin implementation does not transplant Debrify application source.

Distributed under the existing [GNU General Public License v3.0](LICENSE). Personal builds and proposals in this repository are maintained by **IkoMosiEntertainmentHub**.
