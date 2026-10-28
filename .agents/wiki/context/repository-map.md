---
name: agent-wiki-context-repository-map
description: Orientation for an agent opening this repository — what lives where, how to build, test, and run it, the entry points, the generated paths to leave alone, and the traps in the Gradle setup.
---

# Repository Map — mcidentity

Read this before touching anything. It answers *where things are* and *how to
build them*. It does not restate rules — those live in `.agents/rules/` and the
specialized files linked from `.agents/INDEX.md`, and the shared set is served
by the `lxagents-agents-base` connector.

**MCIdentity** is a Minecraft plugin: identity, professions, and stats, running
on SpigotMC, PaperMC, and Folia from a single universal jar. Skills, runes, and
cosmetics are separate add-on repositories and must not be reintroduced here.

## What lives where

| Path | What it is |
|---|---|
| `api/` | Pure Java contracts and models. 12 files, no implementation, no Minecraft types. Published. |
| `common/` | The data layer: storage backends, the two facades, the central-server client. 40 files, 19 tests. Published. |
| `bukkit/` | The shared platform core: lifecycle, commands, listeners, events, schedulers, the `agents` config block. 97 files, 2 test classes. Published. |
| `platforms/spigotmc/`, `papermc/`, `foliamc/` | Per-platform entry points and their resources. Not published; bundled into engine. |
| `platforms/engine/` | The universal entry point. 1 file plus a `plugin.yml`. Produces the only distributed jar. |
| `platforms/` itself | A **container directory**, not a Gradle project. See the trap below. |
| `wiki/` | Human documentation, indexed by `wiki/INDEX.md`. No frontmatter. |
| `.agents/` | Local agent instructions, indexed by `.agents/INDEX.md`. Frontmatter required. |
| `wiki/logs/4/6/1/` | The current version's changelog. Version stays at `4.6.1` without explicit approval. |

Packages are `io.github.mcoriax.mcidentity.<module>`, with `api` the one
exception: it uses `io.github.mcoriax.mcidentity.api` so its contracts do not
collide with the plugin's own root package.

### Duplication that is intentional

Each of `spigotmc`, `papermc`, and `foliamc` carries its own `plugin.yml`,
`config.yml`, `textures.yml`, and 21 default `professions/` YAMLs — 24 files
each. They are copies on purpose: each platform jar must stand alone with its
own descriptor and defaults. Do not deduplicate them, and do not treat a
one-sided edit as a bug until you check whether it was deliberate.

`platforms/engine/` ships **only** a `plugin.yml`. The universal jar gets its
config and professions from whichever platform module the runtime detection
resolves to.

## Build, test, run

Use the committed wrapper. Never invoke a system-installed `gradle`, and never
lower the toolchain — the build is pinned to **Java 25**, auto-provisioned by
the `foojay-resolver` plugin in `settings.gradle`, on **Gradle 9.5.0**.

| Command | What it does |
|---|---|
| `./gradlew build` | Compiles every module and runs the whole test suite. |
| `./gradlew :common:test` | The data-layer suite, including `ArchitectureBoundaryTest`. Fastest useful check. |
| `./gradlew :platforms:engine:shadowJar` | Builds the universal shaded jar. |
| `./gradlew :platforms:engine:exportJar` | Copies that jar to the root `build/`, and to a test server when `ENGINE_PLUGIN_JAR_PATH` is set. Runs automatically — `build.finalizedBy exportJar`. |
| `./gradlew clean` | Deletes the **root** `build/` only, not each module's. |

To exercise the engine jar on a real server, set `ENGINE_PLUGIN_JAR_PATH` to the
server's `plugins/` directory before building; `exportJar` clears stale
`mcidentity*.jar` files there first. One jar must boot on all three platforms
with no code change — `MCIdentityEnginePlugin` detects Folia, then Paper, then
falls back to Spigot, by runtime reflection.

Publishing is `:api:publish`, `:common:publish`, and `:bukkit:publish` only.
Credentials come from the `GITHUB_ACTOR` and `GITHUB_TOKEN` environment
variables, never from a file.

### Where the tests actually are

Tests live in `common` (19), `bukkit` (2, `AgentsSettingsTest` and `PermissionsTest`,
which also reads the four `plugin.yml` files), `platforms/spigotmc`
(1), `platforms/papermc` (1), and `platforms/foliamc` (2). `api` and `platforms/engine`
have **no** test sources. A change to `api`, or to `bukkit` outside its `config`
and `permission` packages, is verified by `ArchitectureBoundaryTest` and by compilation, not by a unit
test of its own.

## Entry points

| Class | Module | Role |
|---|---|---|
| `AbstractMCIdentityPlugin` | `bukkit` | The shared bootstrap. Owns the lifecycle order and the `createScheduler()` hook every entry point overrides; delegates profession files to `ProfessionConfigLoader`, storage start-up and retries to `StorageBootstrap`, and the command and listeners to `ListenerRegistrar` (all package-private). Not an entry point itself. |
| `MCIdentityPlugin` | `platforms/spigotmc` | Spigot entry point; installs `BukkitPlatformScheduler`. |
| `MCIdentityPlugin` | `platforms/papermc` | Paper entry point; installs `BukkitPlatformScheduler`. |
| `MCIdentityPlugin` | `platforms/foliamc` | Folia entry point; installs `FoliaPlatformScheduler`. |
| `MCIdentityEnginePlugin` | `platforms/engine` | The universal entry point; picks the scheduler at enable time by reflection. |

`plugin.yml` names the main class, and `${version}` / `${name}` in it are
interpolated from `gradle.properties` by `processResources`. Editing the version
string in a `plugin.yml` by hand is wrong — it is overwritten on every build.

The single most-used type for third-party plugins is `MCIdentityProvider`, in
`common`. Read
[`wiki/reference/api.md`](../../../wiki/reference/api.md) before changing its
surface.

## Generated paths — do not edit, do not commit

Everything below is produced by the build and is already in `.gitignore`:

* `build/` and every `**/build/` — compiled classes, jars, shaded outputs.
* `.gradle/` — Gradle's own caches and the configuration-cache entries.
* `**/out/` and `/bin/` — IDE output.

There is no checked-in generated source. If a file under one of these paths
appears in a diff, something is wrong with the ignore rules, not with the
build.

There is no `.github/` directory in this repository. `.agents/workflows/ci-pipeline.md`
describes the pipeline this project expects (JDK 25, `:common:test`, shading
validation), but nothing in the working tree runs it — do not cite it as
evidence that a check passed.

## Traps

**A module with no `build.gradle` is skipped silently.** The root `build.gradle`
opens `subprojects` with `if (!file('build.gradle').exists()) return`. That guard
is what keeps `platforms/` — which Gradle creates implicitly from the
`platforms:foliamc` path segment — out of the build. The same early return
decides who publishes. A module can exist on disk, be listed in
`settings.gradle`, and still do nothing. Check for the build file first when a
module appears inert.

**Adding a platform module needs three edits, not one.** Register it in
`settings.gradle`; add it to engine's `dependsOn` loop and `implementation`
list in `platforms/engine/build.gradle`; and add a row to `PLATFORM_SOURCE_ROOTS`
in `ArchitectureBoundaryTest`. That last one is easy to miss — the Gradle
`fileTree` glob covers new modules automatically, so the boundary test re-runs
and *passes*, while quietly not reading the new module's sources. A green test
is not evidence here. Then add the scheduler branch to
`MCIdentityEnginePlugin.detectPlatform`.

**`compileOnly` is the default for shared modules below engine.** The three
platform modules declare `api`, `common`, and `bukkit` as `compileOnly` because
they are bundled into the universal jar; promoting one to `api` or
`implementation` ships a second copy of every class. `compileOnly` also does not
reach the test classpath, which is why each platform module adds
`testImplementation project(':bukkit')` by hand.

**The shaded jar must contain exactly one `plugin.yml`.** Engine's `shadowJar`
excludes every copy that did not come from its own resources output. Adding a
resource with a name that collides with a dependency jar's resource is the usual
way to break this, and the failure appears at server start, not at build time.

**The configuration cache silently degrades the build.** It is on. A build
script that captures a `Project`, a `FileTree`, or a `Configuration` in a
closure, or that uses `File.copyTo` / `File.delete` inside a task action, will
not fail — the cache entry is discarded and every build gets slower. Capture
plain values at configuration time and use `java.nio.file` in task actions.

**`ArchitectureBoundaryTest` is a build-graph citizen.** It reads `bukkit` and
`platforms/*` sources that are not on its classpath, declared as
`inputs.files(...)` in `common/build.gradle`. If that input declaration is lost
in a refactor, the test goes up to date and stops catching violations without
any signal.

## Read next

| When | Read |
|---|---|
| Before any work | [`.agents/rules/repository.md`](../../rules/repository.md) |
| Adding a dependency or changing the build | [`.agents/dependencies/gradle-multi-project-rules.md`](../../dependencies/gradle-multi-project-rules.md) |
| Importing across module boundaries | [`.agents/architecture/module-isolation.md`](../../architecture/module-isolation.md) |
| Touching storage | [`.agents/architecture/storage-abstraction.md`](../../architecture/storage-abstraction.md) |
| What the build does and why | [`wiki/information/project-structure.md`](../../../wiki/information/project-structure.md) |
| What each module does at runtime | [`wiki/information/architecture.md`](../../../wiki/information/architecture.md) |
