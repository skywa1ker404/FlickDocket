# TODOList

Legend: `[x]` done and verified · `[~]` written, **not yet verified by CI/device** · `[ ]` planned. Nothing is marked `[x]` unless it was actually verified.
Last updated: 2026-10-25 (branch `foundation/phase-0`). Last green CI run: 40155464209 (compile, 18 unit tests, assembleDebug, lintDebug).

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

## Phase 2 — TMDB
- [ ] Re-read TMDB terms/rate limits; decide API-key strategy; document in spec
- [ ] Search/discover/detail, posters (Coil), episodes/seasons cache, attribution in About
## Phase 3 — Serializd import
- [ ] Obtain a real export sample; parser + preview + undo
## Phase 4 — Trakt & Simkl
- [ ] Verify docs/ToS; credential store; OAuth/device flows; sync preview + conflicts; disconnect
## Phase 5 — MDBList & ratings
- [ ] Verify terms; API-key entry; cached ratings with source labels
## Phase 6 — Polish
- [ ] Tablet two-pane, calendar/upcoming, own-data export/import, tags/lists, migration tests, accessibility audit

## Known unverified items
- UI behaviour, themes, reduced motion, shared-element transition, accessibility: compiled and linted only, never run on a device or emulator.
- `compileSdk 37` and material3 `1.5.0-alpha29` are pre-release stack choices forced by compose dependencies; revisit when stable.
