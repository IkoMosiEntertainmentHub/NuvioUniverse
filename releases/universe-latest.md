## Changes
- Restored Cast and Production sections to upstream defaults; removed the extra search interface.
- Player opening artwork and media metadata use canonical artwork/backdrops rather than custom shelf posters.
- Transparent scrolling logos now have a black-to-transparent gradient behind them.
- Box Office shows production cost only, with compact amounts such as $100M and $127M. Removed history charts, revenue comparisons and verbose source/disclaimer text.
- Universe labels show relationships without Released/Returning Series status suffixes.
- Release commits now use one signed GitHub Actions pipeline instead of duplicating the full Android build.
- Added reusable fork/release workflow guidelines in Docs/FORK-WORKFLOW-GUIDELINES.txt.

## Validation
The linked Actions run contains the checks and both signed ARM APK builds. Physical-device playback and visual verification remain additional checks.
