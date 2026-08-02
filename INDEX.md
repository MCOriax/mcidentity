---
name: repository-index
description: Entry-point index of the MCIdentity repository's source modules, documentation, and tooling.
---

# Repository Index

This file is the entry point for understanding the project structure. Agents
MUST read it first, and keep it updated whenever the structure or indexed
content of this repository changes. It reflects only the files and directories
that exist in this repository.

Agent rules are not kept in this repository. They live in the portable `.agents`
instruction set used alongside it; the Minecraft platform guidance for this
plugin lives in that set at `platforms/MINECRAFT.md`.

## Root Files

| File | Purpose |
|---|---|
| [`INDEX.md`](INDEX.md) | This project structure index. |
| [`README.md`](README.md) | Human facing project overview. |
| `build.gradle`, `settings.gradle`, `gradle.properties` | Gradle multi project build configuration. |
| `gradlew`, `gradlew.bat`, `gradle/` | Gradle wrapper and version catalog. |
| `.gitattributes`, `.gitignore` | Git configuration. |

## Source Modules

The build is a Gradle multi project defined in `settings.gradle`
(`rootProject.name = mcidentity`). Pure Java modules (`api`, `common`) never
import a platform type; platform code lives in `bukkit` and the three
`platforms:*` entry points. All packages sit under `io.github.mcoriax.mcidentity`.

### `api` — [`api/`](api/)

Pure Java contracts: interfaces and models only, no implementation.

| Package | Purpose |
|---|---|
| `api` | Root contracts, including `IdentityProvider`. |
| `api/model` | Domain model interfaces and value types: `IdentityKey`, `IdentityProperties`, `IdentityStats`, `IdentitySession`, `ActiveIdentitySession`, `Profession`, `ActionType`. |
| `api/db` | The `IMCIdentityDB` persistence contract. |

### `common` — [`common/`](common/)

Cross platform data layer with selectable storage backends: SQLite (the
default), MySQL, PostgreSQL, MongoDB, or the MCIdentity central server.

| Package | Purpose |
|---|---|
| `common` | The two public facades and the package-private helpers behind them. `MCIdentityProvider` is the single entry point for persistence: it builds and owns the backend (`create`), exposes the storage lifecycle (`initializeStorage`, `isRemoteStorage`, `pingStorage`), delegates every data operation, and keeps its `IMCIdentityDB` private so nothing outside this package can reach a backend. `MCIdentityManager` is the single entry point for RAM state, delegating to the package-private `SessionCache`, `ProfessionRegistry`, and `PlayerStateStore`. `DatabaseFactory` (package-private) is the only place naming a concrete backend, and `BaseIdentityRegistrar` (package-private) holds the alternative base identity granting logic. Also `BaseProfession` (constants and helpers for the permanent built-in "player" profession reserved for the primary identity, including the deterministic UUIDs of alternative base identities) and `IdentityAlias` (the shared alphanumeric and case-insensitive uniqueness rule for identity aliases). |
| `common/model` | `MutableIdentityProperties` and other mutable model implementations. |
| `common/central` | `MCIdentityCentralServer`: the `IMCIdentityDB` implementation that calls the central REST server over a pooled `HttpClient`. |
| `common/db` | `StatKeys` (the shared allow-list of stat identifiers), `AbstractSqlDB` (shared JDBC query logic), and the `SQLiteDB`, `MySQLDB`, `PostgreSQLDB`, and `MongoDB` backends. |

Tests live under `common/src/test/java` and cover the provider and manager,
the base profession helpers (including alternative identity UUIDs), the
`StatKeys` allow-list, the `IdentityAlias` rule, the backend factory, the
SQLite backend against a real database file (including the alias round trip,
its case-insensitive uniqueness, and the column back-fill on an older
database), the MySQL and PostgreSQL dialects and the MongoDB backend over
mocked drivers, and (in `common/central`) the central server client exercised
against a stub HTTP server.

`ArchitectureBoundaryTest` enforces the facade rule mechanically: it reads the
`bukkit` and `platforms:*` sources and fails the build when any of them names
`IMCIdentityDB`, a concrete backend, or the `common.db` / `common.central`
packages. Those source trees are declared as inputs of the `:common:test` task,
so a newly introduced violation always re-runs the check rather than being
skipped as up to date.

### `bukkit` — [`bukkit/`](bukkit/)

Shared Bukkit platform core reused by every platform entry point.

| Package | Purpose |
|---|---|
| `bukkit` | `AbstractMCIdentityPlugin`, the shared enable/disable lifecycle, config loading, and central server wiring. |
| `bukkit/commands`, `bukkit/commands/utils` | The `/identity` command executor and its per subcommand helpers. |
| `bukkit/tabcompleter` | Tab completion for `/identity`. |
| `bukkit/listeners`, `bukkit/listeners/utils` | Gameplay listeners: the modular EXP gain listeners grouped by category, identity/inventory/stats listeners, respawn stat restoration (`PlayerRespawnListener`), the identity alias naming in chat and the tab list (`IdentityAliasNameListener`), the central-server cold-boot login protection (`PlayerLoginListener`), and their event handler helpers. |
| `bukkit/event` | Custom Bukkit events (`MCIdentity*Event`) fired for third party plugins. |
| `bukkit/scheduler` | The `PlatformScheduler` abstraction, `Schedulers` registry, and Bukkit implementation. |
| `bukkit/util` | Helpers: experience math, the buffered passive base-profession EXP service, item and skull serialization, cooldown tracking, message components, and more. |

### Platform entry points — [`platforms/`](platforms/)

Each module supplies a concrete plugin entry extending the bukkit core, plus
its `plugin.yml`, `config.yml`, `textures.yml`, and default `professions/`.

| Module | Path | Purpose |
|---|---|---|
| `platforms:spigotmc` | [`platforms/spigotmc/`](platforms/spigotmc/) | SpigotMC entry point (`spigotmc` package) and resources. |
| `platforms:papermc` | [`platforms/papermc/`](platforms/papermc/) | PaperMC entry point (`papermc` package) and resources. |
| `platforms:foliamc` | [`platforms/foliamc/`](platforms/foliamc/) | Folia entry point (`foliamc` package), its regionized scheduler under `foliamc/scheduler`, and resources. |
| `platforms:engine` | [`platforms/engine/`](platforms/engine/) | Universal entry point (`engine` package). `MCIdentityEnginePlugin` detects the running server at enable time and installs the matching scheduler, so a single shaded jar supports SpigotMC, PaperMC, and Folia. Bundles the other three platform modules and carries only its own `plugin.yml`. |

## Documentation

| Directory | Purpose |
|---|---|
| [`wiki/`](wiki/) | Human facing documentation: `requirements.md`, `features.md`, `installation.md`, `environment.md`, `commands.md`, `gui.md`, `custom-exp.md`, `database.md` (data storage on the central server), `api.md`, `bukkit.md`, `system.md`, and `testing.md`. |

## Tooling

| Directory | Purpose |
|---|---|
| `.devcontainer/` | Development container configuration. |
