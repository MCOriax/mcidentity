---
name: feature-implementation-steps
description: Bottom-up implementation template defining contracts before implementations for every new feature.
---

# Planning: Feature Implementation Steps

Force a bottom-up implementation strategy for every new feature. Define
contracts before implementations, and platform-agnostic logic before
platform-specific wiring.

## Template

1. **Define in `api`.** Add or extend interfaces and immutable models
   (`api`, `api/model`, `api/db`). No implementation, no platform types.
2. **Implement the data layer in `common`.** Add persistence in the relevant
   backend(s), keep the `IMCIdentityDB` facade intact, run everything async, and
   validate inputs against `StatKeys`.
3. **Add logic to `bukkit`.** Wire commands, listeners, events, and scheduler
   tasks. Update RAM state first, flush async, and hop to the `PlatformScheduler`
   for world/inventory mutation.
4. **Test across platforms.** Add unit tests under `common/src/test/java`, then
   verify the shaded engine jar on SpigotMC, PaperMC, and Folia.
5. **Sync docs and index.** Update the relevant `wiki/` pages and `INDEX.md`.

Each layer should be reviewable on its own before the next begins.
