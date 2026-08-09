---
name: folia-threading-model
description: Folia region schedulers and the PlatformScheduler abstraction.
---

# Knowledge: Folia Threading Model

Folia replaces the single main thread with region-based parallelism. Tasks must
run on the scheduler that owns the data they touch.

## Folia Schedulers

* **RegionScheduler** — runs a task on the region owning a given location or
  entity's chunk. Use for world/block mutations.
* **EntityScheduler** — runs a task tied to a specific entity, following it
  across region transfers. Use for player inventory, stats, and identity state.
* **GlobalRegionScheduler** — runs global, region-independent tasks (e.g.
  world-wide timers). Use sparingly.

## The PlatformScheduler Abstraction

MCIdentity never calls Bukkit/Folia scheduler APIs directly from core logic.
Instead it uses the `PlatformScheduler` abstraction (`bukkit/scheduler`), with
the concrete implementation selected at enable time via the `Schedulers`
registry:

* SpigotMC / PaperMC — classic main-thread scheduler.
* Folia — regionized scheduler under `platforms/foliamc/scheduler`.

This lets the same core plugin logic run unchanged across Bukkit, Paper, and
Folia. Any new listener or task that mutates world state or inventories MUST hop
back onto the `PlatformScheduler` rather than assuming a single main thread.
Database reads/writes stay off the scheduler entirely (async). Run new code
through `prompts/folia-compatibility-check.md`.
