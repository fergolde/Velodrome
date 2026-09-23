# Image Loading Performance

## Objective

Reduce visible image-loading stutter in Compose screens while preserving authenticated Navidrome artwork and offline cache behavior.

## Problem

Images appear to load in bursts even when Coil cache entries should exist. Current evidence points to unstable request/cache identity, duplicated authentication parameters, many decoded size variants, and no bounded image prefetch.

## Authorized scope

- Branch: `perf/image-loading`
- Local implementation and verification only.
- No push, pull request, merge, release, or GitHub operation without explicit user authorization.
- Keep tests with each behavior change.

## Constraints

- Preserve authenticated artwork requests.
- Do not preload the entire catalog or create an unbounded background workload.
- Keep image decoding sized to the rendered surface.
- Effective TDD: not enabled by project/session configuration; use ordinary focused tests and functional checks.
- Delivery strategy: `ask-on-risk`; no PR planning until user authorizes delivery.

## Tasks

- [x] IMG-01 — Prove and stabilize Coil cache identity; remove duplicate image authentication. Route: delegated direct writer because this spans image loader, interceptor, auth client, and tests. Checks: focused key/interceptor tests and unit test suite.
- [x] IMG-02 — Normalize rendered image sizes and add bounded prefetch for high-traffic lazy surfaces. Route: delegated direct writer because this spans shared Compose components and multiple screens. Checks: focused unit/UI-compilation checks and manual scroll scenario.
- [x] IMG-03 — Isolate avoidable recomposition and improve loading-state continuity with placeholders. Route: delegated direct writer because this spans player and shared image components. Checks: unit/build checks and manual player/scroll scenario.
- [ ] IMG-04 — Measure final behavior, record residual risks, and close work units. Route: inline verification. Checks: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleDebug` where environment permits.

## Acceptance criteria

- Repeated requests for the same artwork and canonical size reuse stable Coil memory/disk entries across credential rotation.
- Image requests carry authentication once and remain valid against Navidrome.
- Fast scrolling does not trigger unbounded image work; near-visible content is prefetched within a bounded window.
- Rendered surfaces do not request substantially larger bitmaps than needed.
- Player progress updates do not recreate unrelated artwork requests.
- Focused tests and build checks report exact observed results; unavailable checks remain explicitly recorded.

## Progress

- Exploration complete: likely causes and affected files mapped.
- Current step: IMG-04.
- Next step: final measurement, residual-risk recording, and work-unit closure.

## Verification evidence

- Base branch: `master` at `fb8f69e`.
- Feature branch created: `perf/image-loading`.
- IMG-01, IMG-02, and IMG-03 source/tests are implemented; final device validation remains pending.
- Work-unit commits required per completed task; commit identities will be recorded here.

## IMG-01 evidence

- Cover-art requests now use unauthenticated base URLs; shared `AuthInterceptor` adds credentials once with `setQueryParameter`.
- Coil requests receive explicit stable memory/disk keys derived from canonical server/artwork/size identity.
- Added interceptor cache-identity tests and duplicate-auth regression coverage.
- Focused check: `./gradlew :app:testDebugUnitTest --tests 'com.fergolde.velodrome.util.NavidromeCoverArtKeyerTest' --tests 'com.fergolde.velodrome.util.NavidromeImageInterceptorTest' --tests 'com.fergolde.velodrome.util.AuthInterceptorTest'` — `BUILD SUCCESSFUL`.
- Parent spot check repeated same command — `BUILD SUCCESSFUL`.
- Runtime harness: `N/A` — no device/server session authorized or available for this local analysis task.
- Rollback boundary: revert IMG-01 changes in `AuthInterceptor.kt`, `NavidromeCoverArtKeyer.kt`, `NavidromeImageInterceptor.kt`, their focused tests, and this evidence block.
- Commit: `4f70b75` (`fix(images): stabilize Coil cache identity`).

## IMG-02 evidence

- Added canonical artwork buckets: `64`, `96`, `128`, `192`, and `512` dp; visible requests and prefetch requests share the same builder.
- Added bounded four-item prefetch with cancellation and deduplication for Home, Explore, Albums, and Artists lazy surfaces.
- Added pure tests for canonical size selection and bounded prefetch ranges.
- Focused check: `./gradlew :app:testDebugUnitTest` — `BUILD SUCCESSFUL`.
- Parent spot check repeated the full unit suite — `BUILD SUCCESSFUL`.
- Build check: `./gradlew :app:assembleDebug` — writer reported `BUILD SUCCESSFUL`; parent rerun `BUILD SUCCESSFUL`.
- Runtime scroll harness: pending — no device/server session available.
- Rollback boundary: revert IMG-02 changes in `ArtworkImage.kt`, `ArtworkPrefetcher.kt`, shared image components, the four lazy screens, their focused tests, and this evidence block.
- Commit: `a697094` (`perf(images): prefetch canonical artwork sizes`).

## IMG-03 evidence

- Isolated 1 Hz playback-position collection inside `PlayerProgress`, preventing unrelated player artwork/queue content from recomposing with each tick.
- Added stable theme-colored Coil placeholders to shared album and artist image components without animations or extra network work.
- Parent spot check: `./gradlew :app:testDebugUnitTest` — `BUILD SUCCESSFUL`.
- Writer build check: `./gradlew :app:assembleDebug` — `BUILD SUCCESSFUL`.
- Runtime player/scroll harness: pending — no device/server session available.
- Rollback boundary: revert IMG-03 changes in `PlayerScreen.kt`, `AlbumCover.kt`, `ArtistAvatar.kt`, and this evidence block.
- Commit: `34f2926` (`perf(images): isolate player artwork recomposition`).
