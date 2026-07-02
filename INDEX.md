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
`platforms:*` entry points. All packages sit under `io.github.mcoriax`.

### `api` — [`api/`](api/)

Pure Java contracts: interfaces and models only, no implementation.

| Package | Purpose |
|---|---|
| `api` | Root contracts, including `IdentityProvider`. |
| `api/model` | Domain model interfaces and value types: `IdentityKey`, `IdentityProperties`, `IdentityStats`, `IdentitySession`, `ActiveIdentitySession`, `Profession`, `Ability`, `AbilityType`, `Rune`, `EquippedAbility`, `ActionType`. |
| `api/db` | The `IMCIdentityDB` persistence contract. |

### `common` — [`common/`](common/)

Cross platform data layer. Persistence is delegated to the MCIdentity central
server; there is no local database.

| Package | Purpose |
|---|---|
| `common` | `MCIdentityProvider` (the `IMCIdentityDB` facade plus RAM session cache sync) and `MCIdentityManager` (in-memory profession config and player state). |
| `common/model` | `MutableIdentityProperties` and other mutable model implementations. |
| `common/db` | `StatKeys`, the shared allow-list of stat identifiers. |
| `common/db/central` | `MCIdentityCentralServer`: the `IMCIdentityDB` implementation that calls the central REST server over a pooled `HttpClient`. |

Tests live under `common/src/test/java` and cover the provider and manager,
the `StatKeys` allow-list, and (in `common/db/central`) the central server
client exercised against a stub HTTP server.

### `bukkit` — [`bukkit/`](bukkit/)

Shared Bukkit platform core reused by every platform entry point.

| Package | Purpose |
|---|---|
| `bukkit` | `AbstractMCIdentityPlugin`, the shared enable/disable lifecycle, config loading, and central server wiring. |
| `bukkit/commands`, `bukkit/commands/utils` | The `/identity` command executor and its per subcommand helpers. |
| `bukkit/tabcompleter` | Tab completion for `/identity`. |
| `bukkit/listeners`, `bukkit/listeners/utils` | Gameplay listeners: the modular EXP gain listeners grouped by category, identity/inventory/stats listeners, and their event handler helpers. |
| `bukkit/event` | Custom Bukkit events (`MCIdentity*Event`) fired for third party plugins. |
| `bukkit/scheduler` | The `PlatformScheduler` abstraction, `Schedulers` registry, and Bukkit implementation. |
| `bukkit/util` | Helpers: experience math, item and skull serialization, cooldown tracking, message components, and more. |

### Platform entry points — [`platforms/`](platforms/)

Each module supplies a concrete plugin entry extending the bukkit core, plus
its `plugin.yml`, `config.yml`, `textures.yml`, and default `professions/`.

| Module | Path | Purpose |
|---|---|---|
| `platforms:spigotmc` | [`platforms/spigotmc/`](platforms/spigotmc/) | SpigotMC entry point (`spigotmc` package) and resources. |
| `platforms:papermc` | [`platforms/papermc/`](platforms/papermc/) | PaperMC entry point (`papermc` package) and resources. |
| `platforms:foliamc` | [`platforms/foliamc/`](platforms/foliamc/) | Folia entry point (`foliamc` package), its regionized scheduler under `foliamc/scheduler`, and resources. |

## Documentation

| Directory | Purpose |
|---|---|
| [`wiki/`](wiki/) | Human facing documentation: `features.md`, `installation.md`, `commands.md`, `gui.md`, `custom-exp.md`, `database.md` (data storage on the central server), `api.md`, `bukkit.md`, and `testing.md`. |

## Tooling

| Directory | Purpose |
|---|---|
| `.devcontainer/` | Development container configuration. |
