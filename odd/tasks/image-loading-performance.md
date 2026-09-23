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
- [ ] IMG-02 — Normalize rendered image sizes and add bounded prefetch for high-traffic lazy surfaces. Route: delegated direct writer because this spans shared Compose components and multiple screens. Checks: focused unit/UI-compilation checks and manual scroll scenario.
- [ ] IMG-03 — Isolate avoidable recomposition and improve loading-state continuity with placeholders. Route: delegated direct writer because this spans player and shared image components. Checks: unit/build checks and manual player/scroll scenario.
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
- Current step: IMG-02.
- Next step: normalize rendered sizes and add bounded image prefetch.

## Verification evidence

- Base branch: `master` at `fb8f69e`.
- Feature branch created: `perf/image-loading`.
- IMG-01 source and tests are implemented; IMG-02/03 remain untouched.
- Work-unit commits required per completed task; commit identities will be recorded here.

## IMG-01 evidence

- Cover-art requests now use unauthenticated base URLs; shared `AuthInterceptor` adds credentials once with `setQueryParameter`.
- Coil requests receive explicit stable memory/disk keys derived from canonical server/artwork/size identity.
- Added interceptor cache-identity tests and duplicate-auth regression coverage.
- Focused check: `./gradlew :app:testDebugUnitTest --tests 'com.fergolde.velodrome.util.NavidromeCoverArtKeyerTest' --tests 'com.fergolde.velodrome.util.NavidromeImageInterceptorTest' --tests 'com.fergolde.velodrome.util.AuthInterceptorTest'` — `BUILD SUCCESSFUL`.
- Parent spot check repeated same command — `BUILD SUCCESSFUL`.
- Runtime harness: `N/A` — no device/server session authorized or available for this local analysis task.
- Rollback boundary: revert IMG-01 changes in `AuthInterceptor.kt`, `NavidromeCoverArtKeyer.kt`, `NavidromeImageInterceptor.kt`, their focused tests, and this evidence block.
- Commit: pending parent commit.
