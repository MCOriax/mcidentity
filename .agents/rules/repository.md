---
name: repository-rules
description: Rules specific to the MCIdentity plugin repository.
---

# Repository-specific rules — mcidentity

This repository is the **MCIdentity Minecraft plugin**: a Gradle multi-project
build (`api`, `common`, `bukkit`, `platforms:*`) that ships identity, profession,
and stat features across SpigotMC, PaperMC, and Folia. Read
[`../../wiki/information/architecture.md`](../../wiki/information/architecture.md)
for the module and package layout before making changes. This file orients you to
the repository; the detailed rules live in the specialized `.agents/` files linked
below.

## Build and test commands

| Command | Purpose |
|---|---|
| `./gradlew build` | Compile every module and run the test suite. |
| `./gradlew :common:test` | Run the data-layer tests, including `ArchitectureBoundaryTest`. |
| `./gradlew :platforms:engine:shadowJar` | Build the universal shaded jar. |

The Gradle wrapper is committed; never invoke a system-installed `gradle`.

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
  same commit. See [`../creators/index-creator.md`](../creators/index-creator.md).
* **Versions.** Never edit `project-version` in `gradle.properties` on your own
  initiative. See [`versioning.md`](versioning.md).
