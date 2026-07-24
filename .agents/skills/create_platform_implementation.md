---
name: create-platform-implementation
description: Skill for adding a new target platform entry point (e.g. Sponge, Fabric) alongside SpigotMC, PaperMC, and Folia.
---

# Skill: Create a Platform Implementation

Add a new target platform entry point (e.g. Sponge, Fabric) alongside the
existing SpigotMC, PaperMC, and Folia modules.

## Steps

1. **New Gradle subproject.** Register the module in `settings.gradle` under
   `platforms:<name>` and add its `build.gradle`. Declare server APIs as
   `compileOnly`. See `dependencies/gradle_multi_project_rules.md`.
2. **Extend the bukkit core.** Provide a concrete plugin entry point extending
   `AbstractMCIdentityPlugin`, in its own `<name>` package.
3. **Implement the scheduler.** Provide a `PlatformScheduler` implementation for
   the platform's threading model and register it in the `Schedulers` registry.
   For region-threaded platforms follow `knowledge/folia_threading_model.md`.
4. **Resources.** Ship the module's `plugin.yml`, `config.yml`, `textures.yml`,
   and default `professions/`.
5. **Bundle in the engine.** Add the module to the `platforms:engine` shadow jar
   so the universal jar can detect and install the matching scheduler at enable
   time. Carry only the engine's own `plugin.yml`; exclude duplicates.
6. **Test across platforms.** Verify the shaded engine jar boots on the new
   platform without altering core plugin logic.
7. **Docs.** Update `INDEX.md`, `wiki/installation.md`, and `wiki/system.md`.
