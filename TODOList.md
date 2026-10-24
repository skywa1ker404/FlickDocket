# TODOList

Legend: `[x]` done and verified · `[~]` written, **not yet verified by CI/device** · `[ ]` planned. Nothing is marked `[x]` unless it was actually verified.
Last updated: 2026-10-24 (branch `foundation/phase-0`).

## Bugs (fixed before features)
- [x] README title said "Stillword" (wrong project) → rewritten as FlickDocket.
- [ ] No other known bugs; the repo contained only the README before this branch. Re-check after first CI run.

## Phase 0 — Specs, licence, CI
- [x] `MASTER_SPEC.md` created (architecture, data model, providers, auth, phases, acceptance criteria, dependencies)
- [x] `LICENSE` = GPL-3.0 (text from Debian `common-licenses`)
- [x] Provider terms research (partial — see spec §4 for what is still unverified)
- [~] GitHub Actions workflow `.github/workflows/android.yml`
- [ ] Gradle wrapper (could not be generated offline; add via CI or locally with `gradle wrapper`)
- [ ] Add GPL header/`SPDX-License-Identifier` convention to new files

## Phase 1 — Foundation (all code below is unverified until CI is green)
- [~] Gradle project, version catalog, Kotlin/Compose/KSP/Room plugins
- [~] Room v1: titles, external_ids, library_entries, watch_events + DAOs + schema export
- [~] DataStore settings (theme, dynamic color, motion preference)
- [~] Repository interface + Room implementation, manual DI container
- [~] Domain rules + JVM unit tests (`DomainRulesTest`, 18 cases)
- [~] Instrumented repository tests (`LibraryRepositoryTest`, needs emulator)
- [~] Theme: light / dark / AMOLED / dynamic color, expressive motion scheme, reduced motion (system + override)
- [~] Components: `ShapeMorphLoader` (all MaterialShapes), `EmptyState`, `PosterCard`/`PosterArt`, pills, section header
- [~] Navigation: bottom nav + detail route, shared-element poster transition
- [~] Screens: Library (search, pills, filter sheet, adaptive grid), Add sheet, Detail (status, watch log, rating, notes, history), Discover placeholder, Stats, Settings (appearance, motion, provider status shell)
- [ ] CI green: `testDebugUnitTest`, `assembleDebug`, `lintDebug` (fix whatever fails; pins in `libs.versions.toml` are unverified)
- [ ] Verify Material3 Expressive API names/opt-ins compile against the resolved version
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
