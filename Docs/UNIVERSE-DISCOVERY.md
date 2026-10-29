# Universe configuration and production discovery

Settings → Layout → Details → Universe uses Nuvio's existing settings rows, chips and switches. Custom Artwork Sources and its six Apply Custom Posters To controls are unchanged.

Each relationship feature and Hide Unreleased Titles is stored independently in the existing profile-scoped details payload. Franchise Collections defaults to OFF, including old payload migration. Generated rails are views, not saved collections: switching them off never changes the library, user collections, watch history or progress. Only nonempty results render in one deduplicated Universe rail.

Discover and manage universes searches TMDB movies and TV shows. Add a title-rooted connected-production universe and independently enable/disable it. This is not a curated list of all named shared universes. Disabling a title-rooted universe suppresses its generated details rails, not the title itself.

IMDb's direct typed edges distinguish sequels, spin-offs and remakes. Bounded indirect links remain labelled franchise connections and do not establish shared continuity. Expansion does not traverse remake/crossover edges. Story prequels cannot be inferred from IMDb's release-order Follows label: a small reviewed primary-source supplement supplies explicit prequel and crossover edges. Coverage is intentionally limited rather than speculative. Source references live alongside each supplementary edge in UniverseEvidence.kt.

TMDB verifies the identity and production status of connection candidates. Untitled/TBA/TBD and rumor-labelled identities are excluded. Planned-only records are omitted without independent announcement evidence. Released titles, returning/ended shows and identified productions in development/production/post-production can be represented using the status actually returned. Provider metadata is not independent official verification and coverage is not exhaustive. No release date is manufactured. Hide Unreleased Titles hides future dates, production/development statuses and unknown release state across all generated Universe rails and related-production search. Direct title searches and explicit normal Details navigation remain available.

Cast and Production use the normal upstream sections and navigation. The fork-specific search controls have been removed.

## Versioning

`universe-version.properties` contains only the independent `personalRevision` and `releaseClassification`. Revision 0 is v1.0.0; 1 is v1.0.1; 10 is v1.1.0; 24 is v1.2.4. The first implementation commit for this system uses revision 1. Each qualifying personally authored content change commit increments it once. Replay during rebase/cherry-pick does not increment it again. Official-only update merges preserve it.

Both Android and shared runtime build files compose `NuvioUniverse-<official version>-(<official build>)-v<personal version>`. Official values come from upstream's Version.xcconfig and its release-time `nuvio.app.versionName` override. The published beta suffix for marketing version 0.6.5 is recorded separately in universe-upstream-label.properties; a changed marketing version automatically uses its own new value unless an explicit upstream release label is supplied. Personal release classification is independent of that suffix.

Install the repository-local commit guard after cloning:

```sh
python scripts/universe_version.py install-hooks
```

If Python is not on PATH, set `git config nuvio.python /absolute/path/to/python`. The guard increments and stages only the ledger for personal content commits. The official-update helper preserves the ledger and checks build wiring after merging. CI checks monotonic versioning, build composition and a real temporary-repository official merge followed by a personal commit. No setup can protect against deliberately deleting the guard/ledger or bypassing CI; branch review must retain these files when resolving conflicts.

Validation is recorded in the associated Actions run. Physical-device UI, authentication and playback testing is additional to host tests and APK compilation. The existing official PR remains a separate focused proposal and is not changed by these personal-fork features.

## Details, financials and skip fallback

Empty/fully filtered Universe results are removed from the outer lazy details list, including its padding. New settings use portrait posters; existing saved choices are preserved. Universe relationship labels appear between artwork and title, followed by year. Landscape cards use their known release year. Custom artwork source selection and screen controls are unchanged.

Appearance → Compact movie/show details uses a two-column grid and retains an independent persistent preference. The scrolling header uses a transparent panel and requests a TMDB clear-logo image; a text title remains the fallback.

Box Office expands to show production costs only. Movie budgets use TMDB, with sourced Wikidata costs when available. Costs use compact currency amounts such as $100M and $127M; unknown costs stay unknown. History charts, revenue/profit comparisons, capture timestamps and explanatory/source paragraphs are removed from the UI. No Box Office Archive requests are made by this section.

Skip reads use IntroDB first, then SkipDB for each missing category, then TheIntroDB v4 for remaining gaps. Valid primary markers and explicit confirmed-absence results are retained. Uncertain/out-of-range, invalid and beyond-duration fallback markers are rejected. Separated credits stay separate. IntroDB post-credit scenes are retained as non-skippable boundaries. The providers do not all distinguish an ending sequence from end credits; the app keeps the provider's contextual marker rather than fabricating separate timestamps.

[IntroDB](https://introdb.app/docs), [SkipDB API and ODbL reciprocity terms](https://skipdb.tv/docs), [TheIntroDB v4](https://theintrodb.org/docs). These integrations only read fallback data; they do not submit timestamps or create provider accounts.

`.github/workflows/universe-release.yml` runs the signed reusable build and publishes with the repository's `GITHUB_TOKEN`, making GitHub Actions the release author. Use a `release:` commit message when bumping the personal ledger, or dispatch Publish NuvioUniverse Release. The four persistent UNIVERSE signing secrets must be configured once; missing secrets stop the job before any unsigned release can be published. The release classification property controls the GitHub flag independently of upstream's version label.

Details labels and values, expanded Box Office text and timeline labels use explicit white foregrounds on the dark Details surface. All enabled Universe relationship types share one deduplicated horizontal rail, with per-card relationship labels and existing independent feature filters. Official Collections remains its own section.

The floating logo uses a black-to-transparent vertical gradient. Player/media artwork prefers canonical metadata and restores raw Continue Watching artwork before launching; custom shelf poster URLs are suppressed in player artwork fallbacks. Universe card captions include relationships only, without production statuses.

Release commits run one signed publishing pipeline; the duplicate direct-push ARM build skips those commits. Reusable project preferences: [FORK-WORKFLOW-GUIDELINES.txt](FORK-WORKFLOW-GUIDELINES.txt).
