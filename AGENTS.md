# Code Review Rules — Velodrome

Conventions for this codebase. These are observed from the existing source, not
aspirational: when a rule here contradicts surrounding code, trust the code and
update this file.

## Language

- **New code, comments and identifiers: English.** The codebase is mid-migration
  — older files carry Spanish KDoc and comments. Do not add new Spanish; do not
  churn existing Spanish into English as a side effect of an unrelated change.
- UI copy lives in `res/values/strings.xml` (Spanish, the default locale) and
  `res/values-en/strings.xml`. A new user-facing string needs both, or lint
  fails with `MissingTranslation`.

## Architecture

Layers, and the dependency direction between them:

```
presentation/  ->  domain/  <-  data/
                                di/ wires everything
util/           shared helpers, no layer owns it
```

- `domain/repository/*.kt` holds interfaces. `data/repository/*Impl.kt` holds
  implementations. Bind them in `di/RepositoryModule.kt` with `@Binds` +
  `@Singleton`.
- Presentation never touches a DAO. It goes through a repository interface or a
  use case in `domain/usecase/`.
- `di/` modules are the only place that knows about both Hilt and `BuildConfig`.
  Anything a test needs to construct should be a plain function or class, not a
  Hilt-provided object.

## Coroutines

- Repositories return `Result<T>` and wrap their body in
  `runCatchingWithCancellation` (`data/remote/SubsonicApiExtensions.kt`).
  Plain `runCatching` swallows `CancellationException` and breaks structured
  concurrency.
- Never swallow `CancellationException`. Catch it, rethrow it, and catch the
  general case separately.
- Long work moves to `Dispatchers.IO` explicitly; do not rely on the default
  dispatcher for disk or network.

## Room

- Queries use bound parameters only. No string interpolation into SQL, ever —
  not for sort columns, not for `IN` lists, not for `LIKE` patterns. Bind with
  `:name`.
- `DELETE FROM <table>` wipes are the account/migration escape hatch, not a
  routine operation.

## Testing

- JUnit 4 + MockK + `kotlinx-coroutines-test` + Turbine. Tests live in
  `app/src/test/` mirroring the production package.
- **Extract pure functions to make logic JVM-testable.** Android framework
  classes and Media3 types like `Player.Commands.Builder` throw
  `ExceptionInInitializerError` in a plain JVM test. Keep the decision in a
  top-level function returning plain data (`computeResumptionPlan`,
  `untrustedTransportCommands`) and keep the framework glue thin.
- **A test that cannot fail is worse than no test.** When writing a test around
  an interceptor or a chain, assert the precondition — that the sensitive value
  really did reach the layer under test — before asserting it was redacted or
  absent. `HttpLoggingSanitizationTest` shipped for months passing in empty
  because it built its `Response` with the pre-auth request.
- After writing a test for a fix, revert the fix and confirm the test fails.
  Then restore it.

## Security invariants

These are load-bearing. Changing them requires understanding the blast radius.

- **The Subsonic token never leaves the configured server host.**
  `AuthInterceptor` only attaches `u`/`t`/`s` when the request host matches
  `serverUrl`. A `MediaItem` URI is readable by any controller connected to the
  media session, so `getStreamUrl()` must never embed credentials. Do not widen
  either without re-reading `AuthInterceptor.isRequestForConfiguredServer`.
- **Media session commands are split by trust.** Trusted controllers (system,
  `MEDIA_CONTENT_CONTROL`, own app) get the full set. Untrusted ones (watch
  companions like Garmin Connect) get read + transport only — never
  `SET_MEDIA_ITEM` or `CHANGE_MEDIA_ITEMS`, which would let any installed app
  push a URI into the player. `untrustedTransportCommands()` is the allowlist.
  Rejecting untrusted controllers outright is NOT acceptable: it mutes the watch.
- **On-disk caches are account-scoped.** Audio and artwork cache keys include
  `CredentialsManager.getAccountScope()`. Both the writer and the reader must
  build keys through the shared function — `NavidromeCacheKeyFactory.trackCacheKey()`
  exists because a hand-rolled copy of the key string silently broke offline
  detection.
- **Logging redaction is not cosmetic.** `createHttpLoggingInterceptor` must keep
  `redactQueryParams("u", "t", "s")`. The interceptor sits above the auth
  interceptor, so the response log line already contains the token.
- Credentials live in `EncryptedSharedPreferences` only. Anything else
  password-shaped does not go to disk.

## Git

- Conventional Commits. The security work was split so each commit is
  independently reviewable and each test suite change is meaningful.
- One work unit per commit. If a commit cannot be built and tested on its own,
  it is two commits.
- Update `versionName` in `app/build.gradle.kts` when shipping user-visible
  behavior changes; the repo's precedent bundles that with dependency bumps.
