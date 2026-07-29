---
name: repository-rules
description: Rules specific to the MCIdentity plugin repository.
---

# Repository-specific rules — mcidentity

This repository is the **MCIdentity Minecraft plugin**: a Gradle multi-project
build (`api`, `common`, `bukkit`, `platforms:*`) that ships identity, profession,
and stat features across SpigotMC, PaperMC, and Folia. Read [`INDEX.md`](INDEX.md)
for the module and package layout before making changes. This file orients you to
the repository; the detailed rules live in the specialized `.agents/` files linked
below.

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
  [`../architecture/module_isolation.md`](../architecture/module_isolation.md).
* **Storage facade.** All persistence goes through the `IMCIdentityDB` facade. See
  [`../architecture/storage_abstraction.md`](../architecture/storage_abstraction.md).
* **Async + Folia.** Never block the game thread; route world/inventory work
  through the `PlatformScheduler`. See
  [`../performance/async_first_policy.md`](../performance/async_first_policy.md) and
  [`../knowledge/folia_threading_model.md`](../knowledge/folia_threading_model.md).
* **Bottom-up changes.** Define contracts in `api` before implementing in
  `common`/`bukkit`. See
  [`../planning/feature_implementation_steps.md`](../planning/feature_implementation_steps.md).
* **Docs and index.** Keep the `wiki/` pages and `INDEX.md` current with any
  structural or user-facing change.
