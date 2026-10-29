# Universe configuration and production discovery

Settings → Layout → Details → Universe uses Nuvio's existing settings rows, chips and switches. Custom Artwork Sources and its six Apply Custom Posters To controls are unchanged.

Each relationship feature and Hide Unreleased Titles is stored independently in the existing profile-scoped details payload. Franchise Collections defaults to OFF, including old payload migration. Generated rails are views, not saved collections: switching them off never changes the library, user collections, watch history or progress. Only nonempty relationship rails render.

Discover and manage universes searches TMDB movies and TV shows. Add a title-rooted connected-production universe and independently enable/disable it. This is not a curated list of all named shared universes. Disabling a title-rooted universe suppresses its generated details rails, not the title itself.

IMDb's direct typed edges distinguish sequels, spin-offs and remakes. Bounded indirect links remain labelled franchise connections and do not establish shared continuity. Expansion does not traverse remake/crossover edges. Story prequels cannot be inferred from IMDb's release-order Follows label: a small reviewed primary-source supplement supplies explicit prequel and crossover edges. Coverage is intentionally limited rather than speculative. Source references live alongside each supplementary edge in UniverseEvidence.kt.

TMDB verifies the identity and production status of connection candidates. Untitled/TBA/TBD and rumor-labelled identities are excluded. Planned-only records are omitted without independent announcement evidence. Released titles, returning/ended shows and identified productions in development/production/post-production can be represented using the status actually returned. Provider metadata is not independent official verification and coverage is not exhaustive. No release date is manufactured. Hide Unreleased Titles hides future dates, production/development statuses and unknown release state across all generated Universe rails and related-production search. Direct title searches and explicit normal Details navigation remain available.

Cast/Production → Search cast and productions provides:

- Actor name search with an explicit person selection and movie/TV cast credits.
- Two-person co-star search using intersection of actual cast credits.
- Character text matching against credit roles for up to 20 identified current-cast members, not a global fictional-character database.
- Paginated movie/TV title search and the current title's enabled confirmed connections.

Results open normal Details navigation; the original page's normal back-stack and scroll state remain in use. Requests are cancellable and bounded. Empty/error responses offer an explanation. General credit matches never become franchise relationships.

## Versioning

`universe-version.properties` contains only the independent `personalRevision` and `releaseClassification`. Revision 0 is v1.0.0; 1 is v1.0.1; 10 is v1.1.0; 24 is v1.2.4. The first implementation commit for this system uses revision 1. Each qualifying personally authored content change commit increments it once. Replay during rebase/cherry-pick does not increment it again. Official-only update merges preserve it.

Both Android and shared runtime build files compose `NuvioUniverse-<official version>-(<official build>)-v<personal version>`. Official values come from upstream's Version.xcconfig and its release-time `nuvio.app.versionName` override. The published beta suffix for marketing version 0.6.5 is recorded separately in universe-upstream-label.properties; a changed marketing version automatically uses its own new value unless an explicit upstream release label is supplied. Personal release classification is independent of that suffix.

Install the repository-local commit guard after cloning:

```sh
python scripts/universe_version.py install-hooks
```

If Python is not on PATH, set `git config nuvio.python /absolute/path/to/python`. The guard increments and stages only the ledger for personal content commits. The official-update helper preserves the ledger and checks build wiring after merging. CI checks monotonic versioning, build composition and a real temporary-repository official merge followed by a personal commit. No setup can protect against deliberately deleting the guard/ledger or bypassing CI; branch review must retain these files when resolving conflicts.

Validation is recorded in the associated Actions run. Physical-device UI, authentication and playback testing is additional to host tests and APK compilation. The existing official PR remains a separate focused proposal and is not changed by these personal-fork features.
