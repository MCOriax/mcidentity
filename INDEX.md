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
(`rootProject.name = mcidentity`).

| Module | Path | Purpose |
|---|---|---|
| `api` | [`api/`](api/) | Pure Java contracts: interfaces and models only. |
| `common` | [`common/`](common/) | Cross platform data layer. Persistence is delegated to the MCIdentity central server through the REST client under `common/.../db/central`. |
| `bukkit` | [`bukkit/`](bukkit/) | Bukkit platform core: commands, events, listeners, scheduler, util. |
| `platforms:spigotmc` | [`platforms/spigotmc/`](platforms/spigotmc/) | SpigotMC specific plugin entry and resources. |
| `platforms:papermc` | [`platforms/papermc/`](platforms/papermc/) | PaperMC specific plugin entry and resources. |
| `platforms:foliamc` | [`platforms/foliamc/`](platforms/foliamc/) | Folia specific plugin entry, regionized scheduler, and resources. |

## Documentation

| Directory | Purpose |
|---|---|
| [`wiki/`](wiki/) | Human facing documentation (features, installation, commands, GUI, custom EXP, database, API, Bukkit events, testing). |

## Tooling

| Directory | Purpose |
|---|---|
| `.devcontainer/` | Development container configuration. |
