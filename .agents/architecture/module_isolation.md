---
name: module-isolation
description: Constraint keeping api and common pure Java, with Minecraft types confined to platform modules.
---

# Architecture: Module Isolation

Strict architectural constraint on module boundaries.

## Rule

* The `api` and `common` modules are **pure Java**. They must NEVER import a
  Bukkit, Paper, Spigot, or Folia type.
* All Minecraft-specific logic is confined to the `bukkit` module and the
  `platforms:*` entry points.

## Why

* Keeps the data layer and contracts unit-testable without a running server.
* Lets the same core run unchanged across SpigotMC, PaperMC, and Folia.
* Prevents platform quirks from leaking into persistence or the public API.

## Enforcement

* If core logic seems to need a Minecraft type, invert the dependency: expose a
  pure abstraction from `api`/`common` and implement it in `bukkit`.
* Threading concerns belong to the `PlatformScheduler` abstraction, not to
  `common`.
