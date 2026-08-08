# System Architecture

[← Back to README](../../README.md)

This document describes how the **MCOriax Identity Plugin** is structured: its
Gradle modules, the dependency rules between them, the storage abstraction, and
how the platform entry points fit together. For configuration see
[Installation & Setup](../environments/installation.md) and [Data Storage](../reference/database.md); for the
public integration surface see [Developer API Usage](../reference/api.md).

**Contents**

1. [Architectural overview](#1-architectural-overview)
2. [Modules](#2-modules)
3. [Dependency rules](#3-dependency-rules)
4. [Storage layer](#4-storage-layer)
5. [Bukkit platform layer](#5-bukkit-platform-layer)
6. [Platform entry points](#6-platform-entry-points)
7. [Execution model](#7-execution-model)

---

## 1. Architectural overview

The plugin is a **Gradle multi project** build defined in `settings.gradle`
(`rootProject.name = mcidentity`). All packages sit under `io.github.mcoriax.mcidentity`.
The design separates pure Java contracts and data handling from platform
specific code:

```
api        pure Java contracts (interfaces + models, no implementation)
  ▲
common     cross platform data layer (storage backends, managers)
  ▲
bukkit     shared Bukkit platform core (lifecycle, commands, listeners, events)
  ▲
platforms  spigotmc | papermc | foliamc entry points
```

Higher modules depend on lower ones, never the reverse. The pure Java modules
(`api`, `common`) never import a platform type.

---

## 2. Modules

### `api` — [`api/`](../api/)

Pure Java contracts: interfaces and models only, no implementation.

- `api` — root contracts, including `IdentityProvider`.
- `api/model` — domain model interfaces and value types (`IdentityKey`,
  `IdentityProperties`, `IdentityStats`, `IdentitySession`, `Profession`,
  `ActionType`, and more).
- `api/db` — the `IMCIdentityDB` persistence contract.

### `common` — [`common/`](../common/)

The cross platform data layer with selectable storage backends.

- `common` — the two facades and the package-private helpers behind them:
  `MCIdentityProvider` (persistence plus RAM session cache sync),
  `MCIdentityManager` (in memory profession config, player state, cached base
  slot counts), `DatabaseFactory`, `SessionCache`, `ProfessionRegistry`,
  `PlayerStateStore`, `BaseIdentityRegistrar`, and `BaseProfession` (the
  hardcoded `player` profession and deterministic alternative identity UUIDs).
- `common/model` — mutable model implementations.
- `common/central` — `MCIdentityCentralServer`, the `IMCIdentityDB`
  implementation that calls the central REST server over a pooled `HttpClient`.
- `common/db` — `StatKeys` (the shared stat allow list), `AbstractSqlDB`, and
  the `SQLiteDB`, `MySQLDB`, `PostgreSQLDB`, and `MongoDB` backends.

### `bukkit` — [`bukkit/`](../bukkit/)

The shared Bukkit platform core reused by every entry point (see
[Bukkit platform layer](#5-bukkit-platform-layer)).

### `platforms` — [`platforms/`](../platforms/)

The three concrete plugin entry points (see
[Platform entry points](#6-platform-entry-points)).

---

## 3. Dependency rules

- `api` and `common` are pure Java and must never import a Bukkit or platform
  type; they can be unit tested without a server.
- Platform code lives only in `bukkit` and the `platforms:*` modules.
- Every storage backend implements the same `IMCIdentityDB` contract from `api`,
  so gameplay code never knows which storage is active.
- The `api`, `common`, and `bukkit` modules are published to GitHub Packages for
  third party consumers; the platform jars are distributed as plugin artifacts.

### The two facades

`MCIdentityProvider` and `MCIdentityManager` are the **only** entry points into
identity data and state. Everything else — commands, listeners, GUIs, the
platform entry points, and third party plugins — goes through them:

| Facade | Owns | Reached as |
|---|---|---|
| `MCIdentityProvider` | Persistence: the storage backend, every read and write, and the storage lifecycle. | `MCIdentityProvider.instance` |
| `MCIdentityManager` | Everything held in RAM: active sessions, profession configuration, per player gameplay state. | Static methods |

Three rules keep that boundary intact:

1. **The backend is private.** `MCIdentityProvider` holds its `IMCIdentityDB` in
   a private field and never hands it out. Storage comes up through
   `MCIdentityProvider.create(type, options)`, and its lifecycle is driven with
   `initializeStorage()`, `isRemoteStorage()`, and `pingStorage()` — so no
   platform class ever names a concrete backend or the persistence contract.
2. **The facades delegate, they do not implement.** Each one is a thin surface
   over package-private helpers (`DatabaseFactory`, `SessionCache`,
   `ProfessionRegistry`, `PlayerStateStore`, `BaseIdentityRegistrar`), which are
   unreachable from outside `common` because they are package-private.
3. **The rule is tested, not just documented.** `ArchitectureBoundaryTest` reads
   the `bukkit` and `platforms:*` sources and fails the build when one of them
   mentions `IMCIdentityDB`, a concrete backend, or the `common.db` /
   `common.central` packages. Those trees are declared as inputs of the test
   task, so a new violation always re-runs the check. When it fires, the fix is
   to add what the caller needs to `MCIdentityProvider` — not to relax the rule.

---

## 4. Storage layer

Storage is selected with `database.type` in `config.yml` and hidden behind the
`IMCIdentityDB` contract:

- `SQLiteDB`, `MySQLDB`, and `PostgreSQLDB` share their query logic through
  `AbstractSqlDB` and differ only in schema DDL and upsert dialect.
- `MongoDB` stores each identity as a single document with embedded properties,
  stats, and session state.
- `MCIdentityCentralServer` implements the same contract by sending asynchronous
  JSON requests over one pooled HTTP client, carrying the bearer token from
  `central_server.token`.
- Stat identifiers are validated against the `StatKeys` allow list before use.

The SQLite schema matches the central server's, so a database file can be moved
between a game server and a central deployment. See [Data Storage](../reference/database.md)
for the table list and backend configuration.

---

## 5. Bukkit platform layer

The `bukkit` module holds everything that touches the server API and is shared
by all three entry points:

- `bukkit` — `AbstractMCIdentityPlugin`, the enable/disable lifecycle, config
  loading, and central server wiring.
- `bukkit/commands` (+ `utils`) — the `/identity` command executor and its per
  subcommand helpers; `bukkit/tabcompleter` provides tab completion.
- `bukkit/listeners` (+ `utils`) — gameplay listeners: modular EXP gain
  listeners, identity/inventory/stats listeners, respawn stat restoration, and
  the central server cold boot login protection.
- `bukkit/event` — custom `MCIdentity*Event` types fired for third party
  plugins.
- `bukkit/scheduler` — the `PlatformScheduler` abstraction and `Schedulers`
  registry that lets shared code schedule work without knowing the platform.
- `bukkit/util` — experience math, the buffered passive base profession EXP
  service, item and skull serialization, cooldown tracking, and message helpers.

---

## 6. Platform entry points

Each module in `platforms/` supplies a concrete plugin class extending the
`bukkit` core, plus its own `plugin.yml`, `config.yml`, `textures.yml`, and
default `professions/`:

| Module | Path | Purpose |
|---|---|---|
| `platforms:spigotmc` | [`platforms/spigotmc/`](../platforms/spigotmc/) | SpigotMC entry point and resources. |
| `platforms:papermc` | [`platforms/papermc/`](../platforms/papermc/) | PaperMC entry point and resources. |
| `platforms:foliamc` | [`platforms/foliamc/`](../platforms/foliamc/) | Folia entry point plus its regionized scheduler under `foliamc/scheduler`. |

The `PlatformScheduler` abstraction is what lets Folia's `RegionScheduler` and
`GlobalRegionScheduler` slot in without changing shared code.

---

## 7. Execution model

- **Asynchronous storage.** Every storage operation runs off the main game
  thread on a dedicated worker pool, so the game thread is never blocked.
- **RAM caching.** Player state, profession config, and base slot counts are
  cached in memory by `MCIdentityManager` and synced back to storage.
- **Safe fallbacks.** Failed reads resolve to empty results or zero and are
  logged, so a storage failure degrades gracefully instead of crashing
  gameplay.
- **Buffered passive EXP.** Passive base leveling EXP is buffered per identity
  in memory and flushed to storage in bulk (every five minutes and on quit).
- **Cold boot protection.** With the `central` backend, pre login pings run on
  the async login thread and retry storage initialization automatically, so a
  cold started central server delays startup instead of breaking it.
