# 0002 — Show data as it arrives; no bundled seed data

**Status**: Accepted (2026-11-26).

## Context

The home screen groups Pokémon by type, but a Pokémon's types are only known once its `/pokemon/{id}` response arrives. While the first sync runs, rows and items are inserted above and between what the user is looking at, so the list can shift.

Constraints: only the three endpoints in the spec may be used (no `/type`); the type list must not be hard-coded (Gen 1 actually uses 17 types and no Dark, while `/type` returns 21); the spec asks to display content as soon as it is fetched.

Under "show data as early as possible", only data that is present before the first fetch removes the shift entirely. Everything else either softens it or delays the data.

## Decision

Show each Pokémon as soon as its detail is stored, and soften the shift:

- `LazyColumn` items keyed by type name, `LazyRow` items keyed by Pokémon id, so Compose keeps the first visible item anchored.
- New items use `animateItem`.
- My Pocket sits outside the `LazyColumn` with a fixed height and an empty-state text, so the first capture does not push the list down. Its `LazyRow` is keyed by capture id (UUID).

The interviewers confirmed on 2026-11-26 that a slight shift is acceptable and that bundled data is not wanted.

## Alternatives considered

| Option | Shift | Why not |
| --- | --- | --- |
| Bundled seed in the APK + background refresh | Removed | Interviewers did not want it; about +3h; changes sync semantics to full refresh |
| "New data" banner, apply on tap | None until tapped | New data waits for a tap, against "display as soon as fetched" |
| Loading screen until all details arrive | Removed | First launch blocks for roughly 25 s |
| Placeholder rows for each type | — | Impossible: the set of types is unknown without the details |

## Consequences

- The list can still move slightly during the first sync. How key anchoring behaves when a new type row is inserted at index 0 is not yet verified (manual check).
- Pokémon without a stored detail do not appear in any type row; there is no "unknown" row.
- Keys are also a performance requirement, so the mitigation costs almost nothing.
