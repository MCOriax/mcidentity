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
