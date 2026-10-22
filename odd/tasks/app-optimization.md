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
| T4 | Performance quick wins: runBlocking, durations, border strokes, dispatcher scopes, prefs preload, accent default, queue index map | inline | pending |
| T5 | Cancellation safety: runCatchingWithCancellation in CacheManager/HomeViewModel/SmartRadioEngine | inline | pending |
| T6 | DI/build cleanup: Settings @Binds, BuildConfig injection, gradle.properties, CI lint | inline | pending |
| T7 | Paging invalidation for albums/artists | inline | pending |
| T8 | Compose performance: Home/Explore state slicing, PlayerScreen stability, duration/border memoization | inline | pending |
| T9 | Audio main-thread work: MediaItem construction off main, EQ settings read | inline | pending |
| T10 | Architecture: remove DAO from AudioPlayerService, purify LoginUseCase, presentation/data decoupling | inline | pending |
| T11 | SettingsViewModel single UiState + one-shot logout event + typed state | inline | pending |
| T12 | Tests for uncovered high-risk ViewModels and engines | inline | pending |
| T13 | Final verification: full unit suite, assembleRelease, lint | inline | pending |

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
