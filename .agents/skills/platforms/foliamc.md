# FoliaMC Technical Standards

When working within the `platforms:foliamc` submodule, strictly adhere to the following rules to ensure server stability:

## 1. Regionalized Threading (CRITICAL)
*   **No Global Schedulers**: Methods like `Bukkit.getScheduler()` do not exist or will throw exceptions.
*   **Entity/Player Tasks**: ALWAYS use the entity's scheduler. 
    *   `player.getScheduler().run(plugin, task -> { ... }, null)`
*   **Location/Chunk Tasks**: ALWAYS use the RegionScheduler.
    *   `Bukkit.getRegionScheduler().run(plugin, location, task -> { ... })`
*   **Global Async Tasks**: Use the AsyncScheduler for tasks completely independent of the world state (e.g., Database saves).
    *   `Bukkit.getAsyncScheduler().runNow(plugin, task -> { ... })`

## 2. UI & Messages
*   Similar to PaperMC, STRICTLY utilize the Adventure API (`Component.translatable(...)` and `NamedTextColor`).

## 3. State Integrity
*   Do not share mutable state across different regions without thread-safe collections (e.g., `ConcurrentHashMap`).
*   When teleporting or modifying entities across regions, use the asynchronous teleportation API.
