---
name: repository-rules
description: Rules specific to the MCIdentity plugin repository.
---

# Repository-specific rules — mcidentity

This repository is the **MCIdentity Minecraft plugin**: a Gradle multi-project
build (`api`, `common`, `bukkit`, `platforms:*`) that ships identity, profession,
and stat features across SpigotMC, PaperMC, and Folia. Read
[`../../wiki/information/architecture.md`](../../wiki/information/architecture.md)
for the module and package layout before making changes, and
[`.agents/wiki/context/repository-map.md`](../wiki/context/repository-map.md) for
where things are and how to build them. This file orients you to the repository;
the detailed rules live in the specialized `.agents/` files linked below.

## Build and test commands

| Command | Purpose |
|---|---|
| `./gradlew build` | Compile every module and run the test suite. |
| `./gradlew :common:test` | Run the data-layer tests, including `ArchitectureBoundaryTest`. The fastest useful check. |
| `./gradlew :platforms:engine:shadowJar` | Build the universal shaded jar. |
| `./gradlew :platforms:engine:exportJar` | Copy that jar to the root `build/`, and to a test server when `ENGINE_PLUGIN_JAR_PATH` is set. Runs automatically — engine's `build` is `finalizedBy` it. |
| `./gradlew clean` | Deletes the **root** `build/` only, not each module's. |

The Gradle wrapper is committed; never invoke a system-installed `gradle`. The
build is pinned to Gradle 9.5.0 on a **Java 25** toolchain that the
`foojay-resolver` plugin provisions automatically — see
[`../dependencies/java-25-toolchain.md`](../dependencies/java-25-toolchain.md).

Tests exist only in `common` (19), `platforms:spigotmc` (1),
`platforms:papermc` (1), and `platforms:foliamc` (2). A change to `api`,
`bukkit`, or `platforms:engine` has no unit test of its own; it is verified by
compilation and by `ArchitectureBoundaryTest`.

There is no `.github/` directory. The pipeline described in
[`../workflows/ci-pipeline.md`](../workflows/ci-pipeline.md) is what this project
expects, not something the working tree runs — never cite it as evidence that a
check passed.

## Rules

* **No skills here.** The ability/skill system was removed from this repository
  and lives in the standalone `mcoriax/mcidentity-skill` add-on, where a skill's
  data is stored on the `ItemStack` persistent data container rather than in the
  database. Never reintroduce an abilities or skills table, model, GUI, or
  command to this plugin.
* **No runes or cosmetics here.** Runes live in `mcoriax/mcidentity-rune` and
  skins and auras live in `mcoriax/mcidentity-cosmetic`. Each add-on creates and
  owns its own tables, keyed by identity UUID, with no foreign key onto this
  plugin's tables, and reads the equipped identity through the session cache.
  Never reintroduce a rune or cosmetic table, model, event, GUI, or command
  here. This repository owns identity, stats, and session management only.
* **Module boundaries.** Keep `api` and `common` pure Java; confine platform types
  to `bukkit` and `platforms:*`. See
  [`../architecture/module-isolation.md`](../architecture/module-isolation.md).
* **The build enforces the boundary in two more places, both quietly.** A module
  without a `build.gradle` is *skipped* by the root `subprojects` guard, not
  rejected — it can exist on disk and in `settings.gradle` and do nothing. And
  `ArchitectureBoundaryTest` reads the `bukkit` and `platforms:*` sources as
  declared task inputs, so it goes up to date and stops catching violations if
  that input declaration is lost. A green boundary test proves nothing unless
  the inputs are still declared. See
  [`../dependencies/gradle-multi-project-rules.md`](../dependencies/gradle-multi-project-rules.md).
* **One universal jar, one `plugin.yml`.** `platforms:spigotmc`, `:papermc`, and
  `:foliamc` are not distributed; they are bundled into `platforms:engine` and
  must keep their siblings on `compileOnly`. Anything published as a plugin
  artifact is that one shaded jar.
* **Storage facade.** All persistence goes through the `IMCIdentityDB` facade. See
  [`../architecture/storage-abstraction.md`](../architecture/storage-abstraction.md).
* **Async + Folia.** Never block the game thread; route world/inventory work
  through the `PlatformScheduler`. See
  [`../performance/async-first-policy.md`](../performance/async-first-policy.md) and
  [`../knowledge/folia-threading-model.md`](../knowledge/folia-threading-model.md).
* **Bottom-up changes.** Define contracts in `api` before implementing in
  `common`/`bukkit`. See
  [`../planning/feature-implementation-steps.md`](../planning/feature-implementation-steps.md).
* **Docs and index.** Keep the `wiki/` pages current with any structural or
  user-facing change, and update the index that owns the changed scope in the
  same commit. See `{shared}/creators/index-creator.md`.
* **Versions.** Never edit `project-version` in `gradle.properties` on your own
  initiative. See `{shared}/rules/versioning.md`.

## What "verify" means here

The shared task workflow says to finish and verify each task before starting the
next. In this repository verifying means the Gradle build and the test suite pass
for the modules you touched — `./gradlew build`, or `./gradlew :common:test` when
the change is confined to the data layer.

## Version carriers in this repository

`{shared}/rules/versioning.md` gates every one of these; this table says where
they actually are.

| Carrier | Where |
|---|---|
| `project-version` | `gradle.properties` |
| Plugin manifest version | `platforms/*/src/main/resources/plugin.yml`, interpolated from `project-version` |
| Gradle wrapper and plugin versions | `gradle/`, `build.gradle`, `settings.gradle` |
| Log directories | `wiki/logs/{Major}/{Minor}/{Patch}/` |
| Git tags `v{Major}.{Minor}.{Patch}` | created by `.github/workflows/release.yml` when the version reaches `master`; release drafts are GitHub releases |

## Blast radius, on every branch and pull request

Reviewers need to know how far a change reaches, so say which of these it is:

* **Engine / cross-platform adjustment** — touches `bukkit` schedulers or any
  `platforms:*` module, and therefore the shaded engine jar. Call this out
  explicitly in the pull request body.
* **Pure API addition** — confined to `api` and `common`, with no platform
  impact. Lower risk, and reviewable on its own.
