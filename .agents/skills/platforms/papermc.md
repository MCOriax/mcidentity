# PaperMC Technical Standards

When working within the `platforms:papermc` submodule, strictly adhere to the following rules:

## 1. Task Scheduling & Concurrency
*   **Never use `Bukkit.getScheduler()`**.
*   **Player-Specific Tasks**: Always use `player.getScheduler().run(plugin, task -> { ... }, null)`.
*   **Asynchronous Tasks**: Use `plugin.getServer().getAsyncScheduler().runNow(plugin, task -> { ... })` or `runAtFixedRate`.
*   **Main Thread Sync**: Only synchronize to the main thread when absolutely necessary (e.g., modifying inventories or spawning entities).

## 2. UI & Messages
*   **Adventure API**: ALWAYS use `Component.translatable(key, defaultText)`. Never use legacy string color codes (`&` or `§`).
*   **Colors**: Use `NamedTextColor` (e.g., `NamedTextColor.GREEN`).

## 3. Event Handling
*   Register events securely using `plugin.getServer().getPluginManager().registerEvents()`.
*   Use `EventPriority.MONITOR` with `ignoreCancelled = true` when simply observing actions (e.g., logging or awarding EXP) without modifying the event outcome.
