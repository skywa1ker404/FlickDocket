# FlickDocket

A local-first, Android-only tracker for movies and live-action / non-anime animated TV. Built with Kotlin, Jetpack Compose, Material 3 Expressive, Room and MVVM.

- Works fully offline with no account. No ads, telemetry, AI or public profiles. Tracker only: no playback or streaming.
- TMDB is the planned primary metadata provider; Trakt, Simkl and MDBList are optional and planned; Serializd file import comes first.
- Licensed under **GPL-3.0-or-later** (see `LICENSE`).

Status: early foundation, nothing released. See `MASTER_SPEC.md` for the design and `TODOList.md` for verified progress.

Build (needs Android SDK + JDK 17): `gradle :app:assembleDebug :app:testDebugUnitTest`. CI runs on GitHub Actions.

This product will use the TMDB API but is not endorsed or certified by TMDB (attribution will ship with the TMDB integration).
