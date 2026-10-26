# TODOList

Legend: `[x]` done and verified · `[~]` written, **not yet verified by CI/device** · `[ ]` planned. Nothing is marked `[x]` unless it was actually verified.
Last updated: 2026-10-26 (branch `phase-3/sources-tracking`). Last green CI run: 40269841411 (68 unit tests, assembleDebug, lintDebug) (compile, 18 unit tests, assembleDebug, lintDebug).

## Bugs (fixed before features)
- [x] README title said "Stillword" (wrong project) → rewritten as FlickDocket.
- [x] Build failures found and fixed via CI: wrong runner SDK setup, material3 1.4.0 lacked public expressive APIs, compose 1.13 needs AGP 9.1+, missing `togetherWith` import, lint NewApi theme parent.
- [ ] No other known bugs; the repo contained only the README before this branch. Re-check after first CI run.

## Phase 0 — Specs, licence, CI
- [x] `MASTER_SPEC.md` created (architecture, data model, providers, auth, phases, acceptance criteria, dependencies)
- [x] `LICENSE` = GPL-3.0 (text from Debian `common-licenses`)
- [x] Provider terms research (partial — see spec §4 for what is still unverified)
- [x] GitHub Actions workflow `.github/workflows/android.yml` (green; annotates errors, lint and test counts because artifact downloads are blocked from the dev sandbox)
- [ ] Gradle wrapper (CI uses setup-gradle with Gradle 9.6.0; commit a wrapper via a CI step or locally)
- [ ] Add GPL header/`SPDX-License-Identifier` convention to new files

## Phase 1 — Foundation (all code below is unverified until CI is green)
- [x] Gradle project builds on CI: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.3.10 (built-in Kotlin), KSP 2.3.11, material3 1.5.0-alpha29, compileSdk 37 / targetSdk 36 / minSdk 26
- [~] Room v1 (compiles and KSP runs on CI; behaviour only verifiable by the instrumented tests below): titles, external_ids, library_entries, watch_events + DAOs + schema export
- [~] DataStore settings (theme, dynamic color, motion preference)
- [~] Repository interface + Room implementation, manual DI container
- [x] Domain rules + JVM unit tests (`DomainRulesTest`): 18 tests, 0 failures on CI
- [ ] Instrumented repository tests (`LibraryRepositoryTest`) are written but **not run anywhere yet**: add an emulator job to CI
- [~] Theme: light / dark / AMOLED / dynamic color, expressive motion scheme, reduced motion (system + override)
- [~] Components: `ShapeMorphLoader` (all MaterialShapes), `EmptyState`, `PosterCard`/`PosterArt`, pills, section header
- [~] Navigation: bottom nav + detail route, shared-element poster transition
- [~] Screens: Library (search, pills, filter sheet, adaptive grid), Add sheet, Detail (status, watch log, rating, notes, history), Discover placeholder, Stats, Settings (appearance, motion, provider status shell)
- [x] CI green: `testDebugUnitTest`, `assembleDebug`, `lintDebug`
- [x] Material3 Expressive APIs compile against material3 1.5.0-alpha29 (alpha: API churn expected; re-check when 1.5 stabilises)
- [ ] On-device check: themes, reduced motion, TalkBack, 200% font scale, tablet width
- [ ] Add `INTERNET` permission only when Phase 2 starts

## Phase 2 — TMDB (branch `phase-2/tmdb`; CI run 40199863208 green: build, lint, 52 unit tests)
- [x] Re-checked TMDB docs: auth (Bearer token or api_key), rate limiting (legacy limit removed, ~40 req/s upper bound, 429), images (base_url + size + path from /configuration), attribution text. Caching/storage terms could NOT be read (terms page blocks our fetcher): see spec §4
- [x] Key strategy: **user-supplied key only** (free TMDB key or v4 read token). No key is built into the app; an embedded key can be extracted from an APK. Stored AES-256-GCM via Android Keystore in a separate DataStore, excluded from backups
- [x] Client: rate limiter (20/s), 429 retry honouring Retry-After (max 2), TTL cache with stale fallback, typed errors (missing key, unauthorized, not found, rate limited, offline, server, malformed). JVM-tested with a fake HTTP layer (16 tests)
- [x] Mapping: search/trending pages, movie + TV details, cast, key crew, release dates + certifications (region, then US), external IDs (IMDb, TVDB, Wikidata), seasons incl. Specials (season 0, excluded from episode total), episodes. JVM-tested (11 tests)
- [x] Anime heuristic on TV results (Japanese-language animation hidden); movies unfiltered. Heuristic only
- [x] Pagination with dedupe; UI loads more near end of grid
- [~] UI: Discover (search, trending, states for no key / invalid key / offline / empty), TMDB detail (backdrop, cast, crew, releases, IDs, seasons/episodes, add to library), Settings TMDB card (status, save+verify, disconnect, clear cache), Coil posters with generated-art fallback. Compiles and lints; never run on a device
- [~] Room v2 (metadata_cache) + manual migration 1→2; `addFromTmdb` is idempotent and never overwrites a tracked title. Instrumented test written, **not run**
- [x] Attribution text in About, Discover footer and TMDB detail
- [ ] **TMDB logo not added**: TMDB requires an approved logo asset; obtain it from TMDB's brand assets and confirm its licence is compatible before bundling
- [ ] Verify TMDB caching/storage terms by reading the full terms page manually (cache TTLs: search 1h, trending 6h, detail/season 24h, config 7d, hard prune at 7d; cache cleared on disconnect)
- [ ] Migration test needs the committed v1/v2 schema JSON (app/schemas is not committed yet)
- [ ] Refresh stored metadata for library titles; per-episode watch tracking from fetched episodes
- [ ] Emulator CI job to run instrumented tests; on-device checks
## Phase 2b — Selectable metadata source & library details (branch `phase-3/sources-tracking`, awaiting CI)
- [~] Settings switch: keyless TVmaze (TV only, CC BY-SA 4.0 attribution) or optional TMDB (key section shown only when TMDB is selected); TVmaze response schema is unverified against live data.
- [~] Library cards open the same full details page as Discover (cast, seasons, next episode); stored data is shown when the source is offline.
- [~] Next-episode air date (TMDB `next_episode_to_air`, TVmaze earliest upcoming airdate); "+" on cards and details page advances progress, with Undo that restores the previous status; progress bar and "n / total" on cards.
- [~] Layout: removed the doubled status-bar inset, constrained title width on the details header, reserved two title lines on cards so tiles align; not checked on a device.
- [ ] "Ready to watch / Waiting" grouping and "N behind" badge need per-episode air data (planned with the tracking overhaul).
- Cinemeta is **not** integrated: its terms and the project rules (no addons/streaming ecosystem, verified licensed data only) rule it out. Alternatives: TVmaze for TV + TMDB for movies, manual movie entry, or a Wikidata check after reading its terms.
- Design references (AniSync screenshots) inform layout principles only; no branding, names or artwork are copied.
## Phase 3 — Serializd import
- [ ] Obtain a real export sample; parser + preview + undo
## Phase 4 — Trakt & Simkl
- [ ] Verify docs/ToS; credential store; OAuth/device flows; sync preview + conflicts; disconnect
## Phase 5 — MDBList & ratings
- [ ] Verify terms; API-key entry; cached ratings with source labels
## Phase 6 — Polish
- [ ] Tablet two-pane, calendar/upcoming, own-data export/import, tags/lists, migration tests, accessibility audit

## Known unverified items
- A set of unreviewed TMDB files (separate `data/tmdb` stack, build-time key in BuildConfig) was found uncommitted in the working tree on 2026-10-25, not written in this work. They were set aside, not used and not committed; the build-time key conflicted with the no-embedded-secrets rule.
- UI behaviour, themes, reduced motion, shared-element transition, accessibility: compiled and linted only, never run on a device or emulator.
- `compileSdk 37` and material3 `1.5.0-alpha29` are pre-release stack choices forced by compose dependencies; revisit when stable.
