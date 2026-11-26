# 0007 — Contract tests with fakes; no MockWebServer or Robolectric

**Status**: Accepted (2026-11-26).

## Context

Tests should be ones that can actually fail, not ones that only assert a mock was called. The riskiest behavior is resume after a kill, transaction boundaries, retries, and partial data. Tests are run many times a day by agents, which cannot start an emulator on their own. New third-party libraries need explicit approval.

## Decision

| Area | How it runs | Seam |
| --- | --- | --- |
| Domain (`groupByType`) | JVM | Pure function |
| Mappers | JVM | Pure functions |
| Sync | JVM | `SyncStore` (in-memory fake) + `FakePokeApi` that records requested ids |
| Interceptor | JVM | Real `OkHttpClient` + a scripted responder interceptor after the retry interceptor, with response bodies that record `close()`; fake `Sleeper` |
| ViewModel | JVM | Fake use cases; assert on `state` |
| DAO / SQL | Instrumented, only when DAO or SQL changes | Room in-memory database |

Test dependencies: JUnit 4, `kotlinx-coroutines-test`, `androidx.test` runner.

```kotlin
interface SyncStore {
    suspend fun pendingIds(): PendingWork
    suspend fun writeList(pokemon: List<PokemonEntity>)
    suspend fun writeDetail(detail: PokemonDetailEntity, types: List<PokemonTypeEntity>)
    suspend fun writeSpecies(species: PokemonSpeciesEntity)
}
```

The first batch is 10 tests: D1, M1, B1–B4, S1, S2, I1, V1. The full list is in the tech spec.

## Alternatives considered

- **MockWebServer**: adds a real HTTP round trip. Its unique coverage (Retrofit, OkHttp, and JSON wired together) is covered manually through the OkHttp log, and JSON parsing is tested directly against fixtures.
- **Robolectric**: runs Room on the JVM (estimated 15–35 s per run vs 30–60 s instrumented with a running emulator; about 0.5–1h over the data-layer work). It adds a dependency whose SDK support lags new releases, and its SQLite is not the device's.
- **All DB-backed tests instrumented**: every agent run would need an emulator started by hand.
- **Turbine for event flows**: unnecessary once messages are state (0006).

## Consequences

- Sync running against the real database is not covered by an automated test. It is checked manually by killing the process mid-sync (`adb shell am kill`) and reading the OkHttp log.
- `SyncStore` keeps sync independent of Room, which is also a design benefit.
- About 0.5h is spent writing fakes instead of configuring libraries.
