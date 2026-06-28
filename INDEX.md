# Repository Index

This file is the entry point for understanding the project structure. Agents
MUST read it first, and keep it updated whenever the structure or indexed
content of this repository changes. It reflects only the files and directories
that exist in this repository.

For the rules that govern agent behaviour, read [`AGENTS.md`](AGENTS.md). For the
universal project skill, read [`SKILLS.md`](SKILLS.md).

## Root Files

| File | Purpose |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Primary instruction set for all AI agents. |
| [`SKILLS.md`](SKILLS.md) | Universal core skill (`mcidentity-core`). |
| [`INDEX.md`](INDEX.md) | This project structure index. |
| [`CLAUDE.md`](CLAUDE.md) | Claude Code entry pointer into `AGENTS.md`. |
| [`README.md`](README.md) | Human facing project overview. |
| `build.gradle`, `settings.gradle`, `gradle.properties` | Gradle multi project build configuration. |
| `gradlew`, `gradlew.bat`, `gradle/` | Gradle wrapper and version catalog. |
| `.gitattributes`, `.gitignore` | Git configuration. |

## Source Modules

The build is a Gradle multi project defined in `settings.gradle`
(`rootProject.name = mcidentity`).

| Module | Path | Purpose |
|---|---|---|
| `api` | [`api/`](api/) | Pure Java contracts: interfaces and models only. |
| `common` | [`common/`](common/) | Cross platform data layer. Database backends under `common/.../db/` (`sqlite`, `postgresql`, `mongodb`, shared `sql`). |
| `bukkit` | [`bukkit/`](bukkit/) | Bukkit platform core: commands, events, listeners, scheduler, util. |
| `platforms:spigotmc` | [`platforms/spigotmc/`](platforms/spigotmc/) | SpigotMC specific plugin entry and resources. |
| `platforms:papermc` | [`platforms/papermc/`](platforms/papermc/) | PaperMC specific plugin entry and resources. |
| `platforms:foliamc` | [`platforms/foliamc/`](platforms/foliamc/) | Folia specific plugin entry, regionized scheduler, and resources. |

## Agent Directories

Each agent related directory carries its own `README.md` index for its
contents. Do not re index those contents in `AGENTS.md` or `SKILLS.md`.

| Directory | Index | Purpose |
|---|---|---|
| [`.agents/`](.agents/) | [`.agents/README.md`](.agents/README.md) | Context specific agent instructions and workflows. |
| [`.skills/`](.skills/) | [`.skills/README.md`](.skills/README.md) | Platform specific agent skills. |

## Documentation

| Directory | Purpose |
|---|---|
| [`wiki/`](wiki/) | Human facing documentation (features, installation, commands, GUI, custom EXP, database, API, Bukkit events, testing). |

## Tooling

| Directory | Purpose |
|---|---|
| `.devcontainer/` | Development container configuration. |
