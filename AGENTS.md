# AGENTS.md — Velodrome

Android client for Navidrome (Subsonic API). Kotlin 100%, Jetpack Compose +
Material 3, Media3 / ExoPlayer + MediaSession, MVVM + Clean Architecture.
No trackers, no analytics. Credentials encrypted on device, everything points
to the user's own server.

## Stack (see `app/build.gradle.kts`)

- `compileSdk 37`, `minSdk 34`, `targetSdk 37`, Java 21, Compose + BuildConfig.
- Data: Room (with migrations), DataStore, Paging 3.
- Network: Retrofit + OkHttp + Moshi against `data/remote/NavidromeApi.kt`.
- Images: Coil 3. DI: Hilt + KSP. Background: WorkManager.
- Async: Coroutines + Flow.
- Tests: JUnit 4 + MockK + `kotlinx-coroutines-test` + Turbine, in
  `app/src/test/` mirroring the production package. `unitTests.isReturnDefaultValues = true`.
- Lint is blocking: `abortOnError = true`, `checkReleaseBuilds = true`.

## Commands

```bash
./gradlew :app:testDebugUnitTest   # JVM unit tests
./gradlew :app:lintDebug           # must be clean
./gradlew :app:assembleRelease     # signed via Fernando.jks + keystore.properties / STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD env vars
```

Update `versionName`/`versionCode` in `app/build.gradle.kts` when shipping
user-visible behavior changes.

## Structure (`com.fergolde.velodrome`)

```
presentation/  ->  domain/  <-  data/
di/ wires everything. util/ shared helpers, no layer owns it.
```

- `domain/repository/*.kt` interfaces. `data/repository/*Impl.kt`
  implementations (Account, Album, Artist, Auth, Playlist, Scrobble,
  ServerMigration, Settings, Track). Bind in `di/RepositoryModule.kt` with
  `@Binds` + `@Singleton`.
- `domain/model/Models.kt` domain models. `domain/usecase/*UseCases.kt`
  grouped by feature (`XxxUseCase @Inject constructor` + aggregator like
  `TrackUseCases`).
- `data/local/`: `VelodromeDatabase.kt`, `dao/`, `entity/`,
  `mapper/EntityMappers.kt`, `datasource/`, `queue/QueueSnapshotStore.kt`.
- `data/remote/`: `NavidromeApi.kt`, `dto/SubsonicDto.kt` (one DTO per
  resource), `SubsonicApiExtensions.kt` (defines `runCatchingWithCancellation`).
- `data/worker/`: `SyncLibraryWorker.kt`, `ScrobbleWorker.kt` —
  `CoroutineWorker` with `@AssistedInject`, `withContext(Dispatchers.IO)`.
- `presentation/`: `VelodromeApp.kt` owns the `NavHost`
  (type-safe `@Serializable` routes in `navigation/Routes.kt`, not in
  `MainActivity`); `screen/<feature>/` holds Screen + ViewModel per feature
  (home, explore, albums, artists, login, settings, albumdetail, artistdetail,
  playlistdetail); `audio/` (AudioPlayerManager/Service, ScrobbleManager,
  SmartRadioEngine, TasteProfile), `player/` (PlayerViewModel,
  SharedPlayerViewModel, PlayerScreen), `components/` (GlobalScaffold,
  MiniPlayer, UniversalOptionsSheet).
- `di/`: App, Audio, Database, Dispatcher, Repository, Settings modules +
  `CredentialsEntryPoint.kt`. Only `di/` knows about both Hilt and
  `BuildConfig`. Anything a test needs to construct must be a plain function
  or class, not a Hilt-provided object.
- `util/`: `AuthInterceptor.kt`, `CredentialsManager.kt`,
  `NavidromeCacheKeyFactory.kt`, `NavidromeImageInterceptor.kt`,
  `ServerVersion.kt`, `TimeFormatters.kt`.
- `ui/theme/Theme.kt`. UI copy in `res/values/strings.xml` (Spanish default)
  + `res/values-en/strings.xml`.

Read first: `AGENTS.md` review rules, then `app/build.gradle.kts`,
`domain/repository/TrackRepository.kt`, `data/repository/TrackRepositoryImpl.kt`,
`data/remote/SubsonicApiExtensions.kt`, `util/AuthInterceptor.kt`,
`di/RepositoryModule.kt`, `presentation/navigation/Routes.kt` +
`presentation/VelodromeApp.kt`, one DAO/entity pair, and the mirror tests
`presentation/audio/SessionTrustSplitTest.kt`, `util/AuthInterceptorTest.kt`.

## Language

- **Identifiers and code: English. Comments and KDoc: Spanish.** Older files
  are mid-migration and mix both — do not churn existing English comments
  into Spanish as a side effect of an unrelated change, but all new comments
  and KDoc go in Spanish.
- A new user-facing string needs both `res/values/strings.xml` and
  `res/values-en/strings.xml`, or lint fails with `MissingTranslation`.

## Architecture rules

- Presentation never touches a DAO. It goes through a repository interface or
  a use case in `domain/usecase/`.
- DTOs stay in `data/remote/`; entities in `data/local/`; mappers in
  `data/local/mapper/EntityMappers.kt`. Never leak DTOs/entities into
  presentation.
- Repos annotated `@OptIn(UnstableApi::class)` only where they touch Media3.

## Coroutines

- Repositories return `Result<T>` and wrap their body in
  `runCatchingWithCancellation` (`data/remote/SubsonicApiExtensions.kt`).
  Plain `runCatching` swallows `CancellationException` and breaks structured
  concurrency.
- Never swallow `CancellationException`. Rethrow it, catch the general case
  separately.
- Long work moves to `Dispatchers.IO` explicitly; do not rely on the default
  dispatcher for disk or network.

## Room

- Queries use bound parameters only. No string interpolation into SQL, ever —
  not for sort columns, not for `IN` lists, not for `LIKE` patterns. Bind with
  `:name`.
- `DELETE FROM <table>` wipes are the account/migration escape hatch, not a
  routine operation.

## Testing

- **Extract pure functions to make logic JVM-testable.** Android framework
  classes and Media3 types like `Player.Commands.Builder` throw
  `ExceptionInInitializerError` in a plain JVM test. Keep the decision in a
  top-level function returning plain data (`computeResumptionPlan`,
  `untrustedTransportCommands`) and keep the framework glue thin.
- **A test that cannot fail is worse than no test.** Around an interceptor or
  chain, assert the precondition — that the sensitive value really reached the
  layer under test — before asserting it was redacted or absent.
  `HttpLoggingSanitizationTest` shipped for months passing empty because it
  built its `Response` with the pre-auth request.
- After writing a test for a fix, revert the fix and confirm the test fails.
  Then restore it.

## Security invariants (load-bearing)

- **The Subsonic token never leaves the configured server host.**
  `AuthInterceptor` only attaches `u`/`t`/`s` when the request host matches
  `serverUrl`. A `MediaItem` URI is readable by any controller connected to the
  media session, so `getStreamUrl()` must never embed credentials. Do not widen
  either without re-reading `AuthInterceptor.isRequestForConfiguredServer`.
- **Media session commands are split by trust.** Trusted controllers (system,
  `MEDIA_CONTENT_CONTROL`, own app) get the full set. Untrusted ones (watch
  companions like Garmin Connect) get read + transport only — never
  `SET_MEDIA_ITEM` or `CHANGE_MEDIA_ITEMS`. `untrustedTransportCommands()` is
  the allowlist. Rejecting untrusted controllers outright is NOT acceptable.
- **On-disk caches are account-scoped.** Audio and artwork cache keys include
  `CredentialsManager.getAccountScope()`. Writer and reader must build keys
  through `NavidromeCacheKeyFactory.trackCacheKey()` — a hand-rolled copy of
  the key string silently broke offline detection once.
- **Logging redaction is not cosmetic.** `createHttpLoggingInterceptor` must keep
  `redactQueryParams("u", "t", "s")`. It sits above the auth interceptor, so
  the response log line already contains the token.
- Credentials live in `EncryptedSharedPreferences` only. Anything else
  password-shaped does not go to disk.

## Git

- Conventional Commits. One work unit per commit: if it cannot be built and
  tested on its own, it is two commits. Keep each commit independently
  reviewable with its tests alongside the behavior.
