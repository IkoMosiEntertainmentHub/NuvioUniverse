# Contributing to Nuvio Universe

This repository holds personal Nuvio Mobile builds and focused proposals for official Nuvio. Bug fixes, Universe improvements, documentation and translations are welcome for discussion here.

## Before changing code

Describe the problem and expected behavior in an issue or PR. Keep one concern per change. For UI changes, include screenshots and identify the build they came from. Do not present older-build screenshots as verification of newer behavior.

Preserve the official project's copyright notices and GPL license. Never commit account passwords, tokens, private signing keys or service-role credentials.

## Validation

Run tests appropriate to the change. Universe service and settings checks:

```bash
./gradlew :composeApp:testAndroidHostTest --tests 'com.nuvio.app.features.details.UniverseRepositoryTest' --tests 'com.nuvio.app.features.details.UniverseSettingsTest' -Pnuvio.android.distribution=full -Pnuvio.ios.distribution=appstore
```

For app changes, also build the Android app and describe real-device checks separately from automated tests. State what remains unverified.

## Personal builds

Use `cmp-rewrite` for the personal version. Retain the separate app identity, this repository's update feed and ARMv7/ARM64 packaging. Keep the official build number and increment the personal version when releasing personal changes. Production updates must reuse the same private signing key.

## Official Nuvio contributions

Prepare a focused branch based on current official `cmp-rewrite`. Exclude personal branding, signing, architecture restrictions, release configuration and unrelated fixes. Follow [official Nuvio's contribution rules](https://github.com/NuvioMedia/NuvioMobile/blob/cmp-rewrite/CONTRIBUTING.md) and its PR template when submitting there.

Approval by this repository's owner is not approval by official maintainers. New upstream features require their explicit approval. Current proposal: [NuvioMobile #2425](https://github.com/NuvioMedia/NuvioMobile/pull/2425).
