---
name: gradle-multi-project-rules
description: Constraints for the Gradle multi-project build — module scoping, the compileOnly rule for bundled modules, the publishing gate, the silent no-build.gradle skip, and engine shading.
---

# Dependencies: Gradle Multi-Project Rules

Constraints for the Gradle multi-project build (`settings.gradle`,
`rootProject.name = mcidentity`). Seven projects, one direction:
`api ← common ← bukkit ← platforms:{spigotmc, papermc, foliamc} ← platforms:engine`.

For what each module *does*, see
[`../../wiki/information/architecture.md`](../../wiki/information/architecture.md);
for what the build *does* with them, see
[`../../wiki/information/project-structure.md`](../../wiki/information/project-structure.md).
This file holds only the constraints a change must not break.

## The module must have a `build.gradle`, or nothing happens

The root build opens `subprojects` with:

```groovy
if (!file('build.gradle').exists()) {
    return
}
```

This is what keeps the `platforms/` container out of the build — Gradle creates
an implicit project for every path segment in an `include 'platforms:foliamc'`
statement, and that project has no build file. The same early return later
decides who publishes.

**A module without a `build.gradle` is skipped silently, not rejected.** It can
sit on disk, be listed in `settings.gradle`, and contribute nothing. When a
module behaves as if it does not exist, check for the build file first. Any new
module needs one.

## Module Rules

* `api` and `common` are pure Java. They must not depend on any platform API.
  The rule and its rationale live in
  [`../architecture/module-isolation.md`](../architecture/module-isolation.md);
  do not restate them here.
* `bukkit` is the shared platform core; `platforms:*` modules extend it.
* `bukkit` is the **only** module that re-exposes its siblings — `api project(':api')`
  and `api project(':common')`. Do not add `api` or `implementation` scoping to
  a module below it. The scoping each module uses is deliberate:

  | Module | Sibling scoping | Why |
  |---|---|---|
  | `api` | none (leaf) | Published library, `java-library`. |
  | `common` | `compileOnly project(':api')` | Must not drag `api` into consumers. |
  | `bukkit` | `api` on `:api` and `:common` | The one re-exposer. |
  | `platforms:spigotmc`, `:papermc`, `:foliamc` | `compileOnly` on all three | They are bundled into engine. |
  | `platforms:engine` | `implementation` on all six | It is the shaded artifact. |

## Platform APIs

* Server platform APIs — Spigot, Paper, Folia — must ALWAYS be declared as
  `compileOnly`. They are provided by the running server at runtime and must
  never be bundled.
* The PostgreSQL and MongoDB drivers are `compileOnly` too, and are fetched by
  the server at runtime from the `libraries:` list in each `plugin.yml`. SQLite
  and MySQL drivers ship with Spigot and Paper, so only those two are listed.
* **The same `compileOnly` rule applies to the shared modules, not only to
  platform APIs.** The three platform modules declare `api`, `common`, and
  `bukkit` as `compileOnly` precisely because they are bundled into the
  universal jar. Promoting one to `api` or `implementation` ships a second copy
  of every class in the final artifact.
* **`compileOnly` does not reach the test classpath.** Each platform module
  therefore adds `testImplementation project(':bukkit')` explicitly, because
  `bukkit` is what re-exposes `api` and `common`. Removing that line breaks the
  platform tests' compilation, not the main one.
* `platforms:engine` compiles against the **Folia API only**. Folia declares
  the `org.spigotmc:spigot-api` capability and so provides every Bukkit, Paper,
  and Folia type the bundled schedulers reference. Declaring all three at once
  is a mutually exclusive capability conflict that Gradle rejects. Platform
  detection is runtime reflection, so no compile-time Paper or Spigot
  dependency is needed.

## Publishing

* Only `api`, `common`, and `bukkit` reach GitHub Packages. The gate is a
  plain `return` in the root `subprojects` block, before
  `apply plugin: 'maven-publish'`. Platform jars are distributed as plugin
  artifacts and are never published. A module added below `bukkit` inherits
  the skip and needs no change.
* `artifactId = "${rootProject.name}-${project.name}"` in the publication block
  is **load-bearing**. Without it each publication would use the bare project
  name and all three would publish a colliding `io.github.mcoriax:api`, so a
  consumer could not depend on the core and an add-on at once. Do not remove or
  bypass the qualification.
* Credentials are read from the `GITHUB_ACTOR` and `GITHUB_TOKEN` environment
  variables. Never hardcode them — see
  [`../security/secret-management.md`](../security/secret-management.md).

## Shading

* The `platforms:engine` module is responsible for shading. It bundles the
  `spigotmc`, `papermc`, and `foliamc` modules into one universal jar.
* All four platform modules set `jar { enabled = false }` and rely on
  `shadowJar` with `archiveClassifier.set('')` and `mergeServiceFiles()`.
* Strictly exclude duplicate `plugin.yml` files during shading — the engine jar
  carries only its own `plugin.yml`. The rule keeps a descriptor only when its
  path starts with engine's own `sourceSets.main.output.resourcesDir`; every
  copy from a dependency jar is excluded. The usual way to break this is a new
  resource whose name collides with one in a dependency jar, and the failure
  surfaces at server start rather than at build time.
* The three platform `shadowJar`s are wired as explicit `dependsOn` of engine's
  `compileJava` and `shadowJar`. They must stay explicit: the platforms disable
  their thin jar, so the shadow jar is the only artifact Gradle can supply, and
  task heuristics do not guarantee the order.
* **No relocations are configured.** The shadow is a plain
  `configurations = [project.configurations.runtimeClasspath]` merge. Bundled
  classes keep their original coordinates, which is correct here because every
  bundled module is part of this plugin and is not shared with another plugin on
  the server. If a shaded dependency is ever added that *is* shared — a library
  another plugin also ships — it must be relocated, because today nothing is.

## ArchitectureBoundaryTest is wired into the build graph

`ArchitectureBoundaryTest` in `common` reads the source files of `bukkit` and
`platforms:*` — none of which are on its compile classpath — and fails the build
when one of them names a storage internal. `common/build.gradle` declares those
trees as `inputs.files(...)` on the `test` task for exactly this reason.

* **Never drop the `inputs.files(...)` block.** Without it Gradle treats the
  test as up to date after the first run and a newly introduced boundary
  violation never executes. The failure mode is silence.
* **Adding a platform module needs a new row in the test's own
  `PLATFORM_SOURCE_ROOTS` array.** The Gradle `fileTree` glob
  (`platforms/*/src/main/java/**/*.java`) picks a new module up automatically,
  so the task re-runs — and passes, while not reading the new module. A green
  boundary test is not evidence that a new module was checked.
* When the test fires, the fix is to add what the caller needs to
  `MCIdentityProvider` — never to relax the rule.

## Adding a module

In order: register it in `settings.gradle`; give it a `build.gradle` or it is
skipped; give it its siblings with `compileOnly` and the server API with
`compileOnly`; add `testImplementation project(':bukkit')` if it has tests; add
it to engine's `implementation` list and its `dependsOn` loop; add a
`PLATFORM_SOURCE_ROOTS` row; add the scheduler branch to
`MCIdentityEnginePlugin.detectPlatform`; and ship its own `plugin.yml`,
`config.yml`, `textures.yml`, and default `professions/`. See
[`../skills/create-platform-implementation.md`](../skills/create-platform-implementation.md).
