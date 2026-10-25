# FlickDocket — Master Specification

Status: living document. Last updated 2026-10-24. Planned work is never marked complete here; see `TODOList.md` for status.

## 1. Product rules (permanent)
- Android-only. Kotlin, Jetpack Compose, Material 3 Expressive, Room, MVVM, Coroutines/Flow.
- Tracker only: no playback, streaming add-ons, media servers or social features.
- Movies and live-action / non-anime animated TV (specials where supported). No anime category.
- Local-first: works fully without any account. No mandatory app account, ads, telemetry, AI or public profiles.
- Never fabricate data or test results. No sample titles ship in the app; empty states are real.
- Preview imports and sync conflicts; never silently overwrite user data.
- Releases are never published without explicit maintainer approval. All commits/pushes are under `skywa1ker404`.
- Licence: **GPL-3.0-or-later**. Every dependency must be GPLv3-compatible (see §10).

## 2. Architecture
Single-module app `skywa1ker404.flickdocket` (split into modules only if build time demands it).

| Layer | Package | Responsibility |
|---|---|---|
| UI | `ui.theme`, `ui.components`, `ui.navigation`, `ui.screens.*` | Compose screens, stateless components, ViewModels (MVVM, `StateFlow`) |
| Domain | `domain` | Models, pure rules (`LibraryRules`, `DraftValidation`, `MotionRules`, `ThemeRules`), provider descriptors. No Android imports, JVM-testable |
| Data | `data.local`, `data.prefs`, `data.repo` | Room DB, DataStore settings, repository interfaces + Room implementation |
| DI | `di.AppContainer` | Manual container; no DI framework (revisit only if graph grows) |

Rules: UI → ViewModel → Repository interface → DAO. Repositories expose `Flow`; writes are `suspend` and transactional. Providers (Phase 2+) implement `MetadataProvider` / `SyncProvider` interfaces behind the repository; UI never talks to a provider directly.

## 3. Data model (Room)
v1 (implemented): `titles`, `external_ids`, `library_entries`, `watch_events`. Schemas exported to `app/schemas` and every change ships with a migration + migration test.
- `titles`: id, mediaType, name, tmdbId?, overview?, posterPath?, backdropPath?, releaseYear?, runtimeMinutes?, totalEpisodes?, source (MANUAL/TMDB), createdAt. Unique (mediaType, tmdbId).
- `external_ids`: titleId, provider, externalId (IMDb/Trakt/Simkl ids only — identifiers, never credentials).
- `library_entries`: titleId (PK), status, rating (0–100, steps of 5), favorite, notes, addedAt, updatedAt.
- `watch_events`: titleId, watchedAt, season?, episode?. Watch count/progress derive from events (single source of truth; rewatches are natural).
Planned: `episodes`/`seasons` cache (Phase 2), `sync_links` (provider ↔ title mapping + remote revision), `sync_conflicts`, `import_batches` (Phase 3/4), `provider_ratings` cache (Phase 5), tags/custom lists (Phase 6).

## 4. Providers (verify before each phase; facts below dated 2026-10-24)
| Provider | Role | Verified | Still to verify before coding |
|---|---|---|---|
| TMDB | Primary metadata, posters, episodes | Free for non-commercial use with attribution; must show "This product uses the TMDB API but is not endorsed or certified by TMDB." plus TMDB logo (less prominent than app logo) in About/Credits; commercial use needs a licence (developer.themoviedb.org FAQ). | Rate limits, image-caching rules, external-ids endpoint, full terms page (robots-blocked for our fetcher — read manually). |
| IMDb ids/ratings | Optional | — | IMDb ids via TMDB external ids only if TMDB terms allow. IMDb **ratings** have no free licensed API; stay disabled until legitimate licensed access exists. |
| Trakt | Optional two-way sync | — | Current OAuth/device-code flow, whether client secret is required (open-source app cannot hide one → decide user-registered app vs embedded), rate limits, VIP limits, ToS for third-party apps. Apiary docs were JS-only to our fetcher. |
| Simkl | Optional two-way sync | Docs moved to api.simkl.org. OAuth 2.0 auth-code and device/PIN flows; PKCE required for both; `client_id` public; scopes `media:read`/`media:write`; access token 7 days, refresh token 180 days (rotating); free tier = 3 unverified apps; V1 API retires ~Apr 2027 (use V2 only). | Required headers, rate limits, sync endpoints, ToS. |
| MDBList | Optional ratings via user API key | Uses an API key. | Terms, rate limits per tier, which rating sources may be displayed/cached and their licences. |
| Serializd | File import only (first) | No official export format or API found. | Obtain a real user-provided export sample before writing a parser. No scraping, no password collection. |

## 5. Authentication & credentials
- Local tracking needs no login. Provider connections are optional and independent.
- OAuth via system browser / Custom Tabs (auth-code + PKCE) or device-code flow, per provider docs. App never sees provider passwords.
- Credentials (tokens, API keys) live in a dedicated store (Android Keystore-backed encrypted storage), separate from the Room DB and DataStore settings. Excluded from backups (`backup_rules.xml`, `data_extraction_rules.xml` already exclude `credentials` paths), never logged, never exported.
- Status per provider: Not connected / Connected (account label, last sync) / Needs attention (expired, revoked, rate-limited). **Disconnect** revokes the token where supported, deletes credentials and sync links, and keeps all local records.

## 6. Sync, import & conflicts
- Sync is opt-in per provider, two-way, using documented endpoints only; incremental where the provider supports it (e.g. last-activity timestamps).
- Every sync/import builds a **preview**: adds, updates, deletes, and conflicts, shown before anything is written. Nothing applies without confirmation. Conflicts show local vs remote side by side; default resolution is "keep local" and is never silent.
- Matching order: stored provider id → TMDB id → IMDb id → exact title+year (flagged low confidence, user confirms).
- Imports are transactional and undoable via `import_batches`.

## 7. Design system (original, informed by AniSync screenshots)
Principles observed (not copied): poster-first dense grids with 2:3 art and rounded corners; status shown as overlay pills; cinematic blurred/gradient backdrops behind headers; pill-shaped navigation and filter chips in horizontally scrolling rows; section headers with a small accent marker; bottom nav with pill indicator; detail screen split into pill tabs; bottom sheets for filters, sharing and quick edits; stat tiles with big numerals; dark-first with AMOLED option; contained, gentle motion.
FlickDocket's own expression: ember/lagoon palette, generated poster art until TMDB, morphing Material shapes as the brand motif (loaders, empty states, section markers), expressive spring motion, `MaterialExpressiveTheme`.
- Themes: System/Light/Dark/AMOLED + optional dynamic color (API 31+).
- Loading: `ShapeMorphLoader` morphs through **all** `MaterialShapes`. Empty states use a slowly rotating shape container. Static fallbacks under reduced motion.
- Responsive: adaptive grids now; list-detail two-pane on tablets/foldables in Phase 6.
- Shared-element poster→detail transition via `SharedTransitionLayout`; disabled under reduced motion.

## 8. Accessibility
Min 48dp targets; labels on all icon buttons; merged semantics + click labels on cards; state never conveyed by colour alone (status pills carry text); font-scale safe layouts (no fixed text heights); heading semantics; reduced motion = system animator-scale 0 **or** in-app override; TalkBack and large-font passes are acceptance gates for each UI phase.

## 9. Phases and acceptance criteria
| Phase | Scope | Acceptance |
|---|---|---|
| 0 | Specs, licence, CI | MASTER_SPEC/TODOList/LICENSE present; CI workflow runs |
| 1 | Foundation + design system: navigation, Room, themes, reduced motion, reusable components, manual add, library, detail, stats, settings shell | CI green (unit tests, debug build, lint); app launches offline; add/edit/watch/rate/delete persists across restarts; all three themes + reduced motion verified on device; no network permission |
| 2 | TMDB: search, discover, detail metadata, posters (Coil), episodes/seasons, attribution | Terms re-verified; API key handling documented (user-supplied key vs bundled — decided in spec first); offline cache; attribution shown; failures degrade to local data |
| 3 | Serializd file import with preview | Real sample file obtained; parser tests; preview lists every change; undo works; no silent overwrite |
| 4 | Trakt + Simkl optional connections, two-way sync | Per-provider docs/ToS re-verified; auth via documented flows; conflict preview; disconnect keeps local data; token storage + backup exclusion tested |
| 5 | MDBList key + provider ratings | Terms verified; key stored in credential store; ratings cached with source labels; hidden if unlicensed |
| 6 | Polish: tablet two-pane, calendar/upcoming, export/import of own data (no credentials), tags/lists, widgets | Accessibility audit; migration tests; release candidate (publishing still needs approval) |

## 10. Dependencies & licensing (GPL-3.0 compatibility)
All Apache-2.0, which is compatible with GPLv3 (not with GPLv2-only): AndroidX (Compose, Material3, Navigation, Lifecycle, Room, DataStore, Activity, Core, graphics-shapes), Kotlin stdlib, kotlinx.coroutines. Test: JUnit 4 (EPL-1.0, test-only, not distributed). Planned: Coil (Apache-2.0), OkHttp (Apache-2.0), kotlinx.serialization (Apache-2.0). **Rule:** before adding any dependency, asset, font or icon, record its licence here and confirm GPLv3 compatibility. Fonts/icons: Material Icons (Apache-2.0); app icon and poster art are original.
Version pins live in `gradle/libs.versions.toml` and are CI-verified (run 40155464209): AGP 9.4.0, Gradle 9.6.0, Kotlin 2.3.10, KSP 2.3.11, material3 1.5.0-alpha29 (expressive APIs are not public in stable 1.4.0; the 1.5 line pulls compose 1.13 which requires AGP 9.1+ and compileSdk 37). The stack is pre-release; track stable 1.5.

## 11. Testing strategy
JVM unit tests for domain rules and (later) parsers/mappers; instrumented Room tests (in-memory DB) for repository behaviour; migration tests from Phase 2; Compose UI tests for key flows from Phase 6. All build/test/lint runs happen on GitHub Actions; results are reported only from CI logs.

## 12. Risks / open questions
Trakt client-secret handling in an open-source app; TMDB API key distribution; IMDb ratings licensing; Serializd data format; Material3 Expressive API churn (opt-in annotations, 1.5 beta); exact BOM/AGP/KSP pins.
