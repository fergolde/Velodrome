# App Optimization (performance, code, security, dead code)

Branch: `perf/app-optimization` | Delivery: local only until explicit permission | Runner: `./gradlew :app:testDebugUnitTest`

## Specs

- **S1** — Execute the approved optimization plan end to end: performance, code quality, security and dead-code removal.
- **S2** — User instruction verbatim: "ejecuta el plan, trabaja en un rama seperada y no subas nada a github sin permiso, lo haremos al acabar todo correctamente."
- **S3** — User instruction verbatim: "la funcionalidad no se toca".
- **S4** — No push, PR, tag or merge until the user explicitly grants permission.
- **S5** — Every behavior-preserving change must keep the unit test suite green and must not alter user-visible behavior.
- **S6** — Decision-gated items (cleartext policy, `allowBackup`, error copy, Room schema migration) are not implemented without explicit approval; they are reported as pending decisions.
- **S7** — Security invariants in `AGENTS.md` must remain intact: token host-binding, MediaSession trust split, account-scoped cache keys, logging redaction of `u`/`t`/`s`.
- **S8** — Dead code removal is limited to high-confidence candidates with a reference check; framework/generated references are preserved.

## Tasks

| ID | Scope | Route | Status |
|----|-------|-------|--------|
| T1 | Baseline: unit tests, lint, release build | inline | pending |
| T2 | Security hardening: profileable debug-only, URL encoding | inline | done (commit 2) |
| T3 | Dead code: unused use cases, repository overloads, room-paging, datastore core, stale comment | inline | done (commit 1) |
| T4 | Performance quick wins: runBlocking, durations, border strokes, dispatcher scopes, prefs preload, accent default, queue index map | inline | done (commit 3) |
| T5 | Cancellation safety: runCatchingWithCancellation in CacheManager/HomeViewModel/SmartRadioEngine | inline | done (commit 4) |
| T6 | DI/build cleanup: Settings @Binds, BuildConfig injection, gradle.properties, CI lint | inline | done (commit 5) |
| T7 | Paging invalidation for albums/artists | inline | done (commit 9) |
| T8 | Compose performance: Home/Explore state slicing, PlayerScreen stability, duration/border memoization | inline | partial (immutable states + memoization; slicing pending measurement) |
| T9 | Audio main-thread work: MediaItem construction off main, EQ settings read | inline | pending |
| T10 | Architecture: remove DAO from AudioPlayerService, purify LoginUseCase, presentation/data decoupling | inline | done (commit 7, partial: COD-10 pending) |
| T11 | SettingsViewModel single UiState + one-shot logout event + typed state | inline | partial (typed combine done; logout event pending) |
| T12 | Tests for uncovered high-risk ViewModels and engines | inline | partial (repository contract tests added) |
| T13 | Final verification: full unit suite, assembleRelease, lint | inline | done (commit 9) |

## Log

### L1 — User request (verbatim, 2026-10-22)

"quiero que uses todas las skill de android que tengas disponibles para crear un plan para optimizar la app tanto en rendimiento, codigo y seguridad, quitando el dead code que tengamos. solo crea el plan nada de implementarlo. la funcionalidad no se toca. usa subagentes cuando sea necesario"

### L2 — User authorization (verbatim, 2026-10-22)

"ejecuta el plan, trabaja en un rama seperada y no subas nada a github sin permiso, lo haremos al acabar todo correctamente."

### L3 — Scope decision

Plan produced in the previous turn. Execution starts with a green baseline. Decision-gated items (S6) are deferred and reported, never silently changed. No Gradle artifacts are committed.

### L4 — Baseline evidence

- `./gradlew :app:testDebugUnitTest` → 303 tests, 0 failures (45 result files).
- `./gradlew :app:lintDebug` → FAILS pre-existing: 6 `MissingTranslation` errors (`settings_account`, `retry`, `artists_empty`, `albums_empty`, `home_empty`, `explore_empty`), plus warnings (`ModifierParameter` PlayerScreen:845, `SwitchIntDef` AudioPlayerManager:163, `ObsoleteSdkInt` MainActivity:75, newer dependency versions). Fixing translations and the 3 code warnings is behavior-preserving.
- Working tree had pre-existing unrelated staged changes (`.gga`, `.pi/gentle-ai/sdd-preflight.json`, `openspec/config.yaml`). They are excluded from every commit.

### L5 — Dead code removed (commit 1)

Removed `SyncAlbumsUseCase`, the no-arg `AlbumRepository.syncAlbumsFromServer()` overload, `GetArtistCountUseCase`, `ArtistRepository.artistCount()` and `LocalMusicDataSource.getArtistCount()`. Kept `ArtistDao.getArtistCount()` because `ServerMigrationRepositoryImpl` uses it directly. Removed `room-paging` and the redundant `datastore` core artifact. Updated the three affected tests. Evidence: `./gradlew :app:testDebugUnitTest` → 302 tests, 0 failures (one test retired with its dead use case).

### L6 — Security hardening (commit 2)

`profileable` moved from the main manifest to `app/src/debug/AndroidManifest.xml`; merged debug manifest contains it and merged release manifest does not (verified). Stream and cover-art IDs are URL-encoded with `URLEncoder` (pure JVM, testable). Added two regression tests proving `a&b=c` cannot inject extra query params. `./gradlew :app:processDebugMainManifest :app:testDebugUnitTest` → 304 tests, 0 failures.

### L7 — Performance quick wins (commit 3)

- `AudioPlayerService.setupEqualizer`: removed `runBlocking`; the bass-boost preference is read on `serviceScope` and guarded against a replaced engine.
- `AudioPlayerManager`: playlist mirrored into an id→index map (first occurrence), removing the O(n) `indexOfFirst` on every transition; all playlist mutations keep it in sync.
- Derived values memoized: album/playlist durations, Explore row border, Settings dialog borders (no `@Composable` call inside `remember`).
- `SmartRadioEngine` and `ScrobbleManager` take `@IoDispatcher` from the existing `DispatcherModule`.
- Accent default unified in `DEFAULT_ACCENT_COLOR` in the domain layer, removing the cold-start color flash.
- `VelodromeApp.onCreate` warms the cache-preferences file read by `newImageLoader`.
- Evidence: `./gradlew :app:testDebugUnitTest` → 304 tests, 0 failures.

### L8 — Cancellation safety (commit 4)

Replaced plain `runCatching` around suspend work with `runCatchingWithCancellation` in `CacheManager` (init limit read), `HomeViewModel.loadPlaylists`, and `SmartRadioEngine.loadTasteProfile` (commit 3 file). This keeps `CancellationException` propagating instead of turning it into a failed `Result`. Evidence: full unit suite green.

### L9 — DI/build/CI cleanup (commit 5)

- `SettingsModule` now uses `@Binds @Singleton` like the other repositories; the DataStore provider stays in its companion object.
- `BuildConfig.VERSION_NAME` no longer leaks into presentation: `AppModule` exposes `@Named("appVersion")` and `SettingsViewModel` receives it.
- Lint is green: added the 6 missing English translations and fixed `ModifierParameter` (PlayerScreen), `SwitchIntDef` (AudioPlayerManager) and `ObsoleteSdkInt` (MainActivity). Only dependency-update warnings remain.
- Explicit `lint { abortOnError = true; checkReleaseBuilds = true }` policy and a `:app:lintDebug` CI step.
- Removed stale `gradle.properties` flags.
- Evidence: `./gradlew :app:testDebugUnitTest :app:lintDebug` → BUILD SUCCESSFUL, 304 tests, 0 failures, lint green.

### L10 — Paging invalidation (commit 9)

Added `AlbumDao.observeAlbumCount()` / `ArtistDao.observeArtistCount()` Room `Flow` queries, exposed through `LocalMusicDataSource` and the repository interfaces. `AlbumsViewModel`/`ArtistsViewModel` hold the current `PagingSource`, drop the initial count emission, debounce 500 ms and call `invalidate()` so a completed `SyncLibraryWorker` becomes visible without a process restart. Evidence: unit suite green; end-to-end refresh needs a device with a server sync.

### L11 — Architecture (commit 7)

- `AudioPlayerService` no longer injects `TrackDao`. It calls `TrackRepository.getGenreForTrack()` and `getTrackSizeBytes()`, keeping presentation behind the domain contract.
- `LoginUseCase` no longer imports `android.*`. `AppModule` provides `@Named("isDebuggable")` and `@Named("cleartextNotAllowedMessage")`; the use case stays pure and the login tests construct it without a mocked `Context`.
- Evidence: unit suite green.

### L12 — Compose stability + typed settings state (commit 8)

- Annotated every presentation `*UiState` with `@Immutable` (domain models are `val`-only) so Compose can skip unchanged sections.
- `SettingsViewModel` replaced the 12-flow `combine` + positional `values[N] as T` casts with two typed intermediate flows (`PersistedSettings`, `LocalSettingsState`) combined into `SettingsUiState`.
- Added three contract tests for the new `TrackRepository` methods.
- Evidence: 307 tests, 0 failures; lint green.

### L13 — Final verification

- `./gradlew :app:testDebugUnitTest :app:lintDebug` → BUILD SUCCESSFUL, 307 tests, 0 failures, lint green (only dependency-update warnings).
- `./gradlew :app:assembleRelease` → BUILD SUCCESSFUL with R8 minify + resource shrinking.
- Receipt-driven review is globally disabled (`gentle-ai review mode status` → off); no review transaction started.
- Nothing pushed, no PR, no tag. Branch `perf/app-optimization` is local only.

### L14 — Decisions applied (2026-10-22)

User decisions, verbatim: "1. siempre se va a usar https, tanto en local como en remoto. 2. no hace falta hacer backup de la app. 3. no hay problema si se pierden los datos. hazlo si es necesario 4. lo que tu ves mejor."

- **HTTPS always**: removed the dead `usesCleartextTraffic` placeholder; the manifest now declares `usesCleartextTraffic="false"` for debug and release, and `LoginUseCase` rejects `http://` in every build. Removed the now-unused `@Named("isDebuggable")` provider and collapsed the two HTTP tests into one.
- **No backup**: `allowBackup="false"`; removed `fullBackupContent`/`dataExtractionRules` references and deleted the two XML rule files.
- **Artist index**: `ArtistEntity` now declares `Index("name")`; database version 2 with `MIGRATION_1_2` creating `index_artists_name`; `DatabaseModule` registers it.
- **Error copy**: intentionally unchanged. Mapping raw messages would only churn user-visible text with no performance or security gain; left as a separate UX decision.
- Evidence: 306 tests, 0 failures; lint green; merged debug/release manifests verified (`allowBackup=false`, `usesCleartextTraffic=false`, profileable debug-only); `assembleRelease` successful.
