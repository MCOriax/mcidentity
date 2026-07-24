---
name: async-first-policy
description: Strict policy that no database read or write ever blocks the main Bukkit/Folia thread.
---

# Performance: Async-First Policy

## Strict Rule

* No database read or write may EVER block the main Bukkit/Folia thread.

## How

* All persistence runs on the daemon `ExecutorService` and returns
  `CompletableFuture`.
* Compose async work with `thenApply`/`thenCompose`; do not call `.join()` or
  `.get()` on the game thread.
* Hop back to the `PlatformScheduler` only when the result must modify world
  state, entities, or inventories — and only for that final step.

## Rationale

* Blocking the main thread stalls the whole server (or a whole Folia region).
* Keeping I/O off the game thread is what lets the same logic run safely on
  SpigotMC, PaperMC, and Folia.

Validate new listeners and tasks against `prompts/folia_compatibility_check.md`.
