# 0008 — Compose UI tests for screen behavior JVM tests cannot see

**Status**: Accepted (2026-11-27). Amends [0007](0007-tests-without-extra-libraries.md).

## Context

0007 kept every `:app` test on the JVM: ViewModel tests assert on `state`. Manual verification on 2026-11-27 showed what that leaves out.

- On a first launch in airplane mode, the ViewModel test (V1) proves the state carries `message = Offline`. Whether the Snackbar is actually shown, and whether `MessageShown` is reported back, had no test. Manual screenshots then missed the Snackbar and it was first reported as a bug; only a frame-by-frame look at a screen recording showed it was there.
- Tapping a not-yet-loaded "Evolves from" now shows a Snackbar instead of navigating. That rule lives entirely in the composable, so no JVM test can see it.

## Decision

`:app` adds Compose UI tests (`ui-test-junit4`, plus `ui-test-manifest` for debug), versioned by the Compose BOM. They run as instrumented tests on an emulator.

- Tests pass fake UI state straight to the screen composables. No Hilt, no real repositories.
- Scope is U1–U5 in the tech spec: the Evolves from Snackbar and its no-queue rule, the home offline Snackbar and `MessageShown`, the detail screen's state table, and that the Pokéball sends Capture without opening the detail screen.
- They run only in stages that change composables under `ui/`: `./gradlew :app:connectedDebugAndroidTest`.

## Alternatives considered

- **Robolectric**: still excluded, for the reasons in 0007.
- **Hilt testing and navigation-testing for end-to-end screen tests**: two more libraries, and navigation guards and the `onStart` trigger are already checked by hand.
- **Keep relying on manual verification**: the airplane-mode run above is the counterexample. A manual check got even "is it on screen" wrong.

## Consequences

- Each run needs an emulator started by hand, about a minute more per UI stage. Agents cannot start one themselves, so the PO queues it.
- Navigation, animations, scroll position and how long a Snackbar stays visible are still manual. The test clock is virtual: on a debug build in the emulator, main-thread stalls during a cold start cut the 4-second Snackbar to about 2 seconds on screen, and a Compose test cannot see that.
