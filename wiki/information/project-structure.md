# Project Structure

[← Back to README](../../README.md)

This document explains how the build is put together: the seven Gradle
projects, the order they depend on each other, why the dependency scoping
differs from module to module, which modules are published, and how the single
universal jar is produced. For what each module *does* at runtime see
[System Architecture](architecture.md); for configuration see
[Installation & Setup](../environments/installation.md).

**Contents**

1. [The seven projects](#1-the-seven-projects)
2. [Layer order](#2-layer-order)
3. [Dependency scoping](#3-dependency-scoping)
4. [The build graph as a citizen](#4-the-build-graph-as-a-citizen)
5. [What gets published](#5-what-gets-published)
6. [The universal jar](#6-the-universal-jar)
7. [Configuration cache](#7-configuration-cache)

---

## 1. The seven projects

`settings.gradle` declares seven projects. There is no eighth: `platforms/` is
a container, not a project.

```
mcidentity                      root, no source
├── api                         12 .java   pure Java contracts
├── common                      40 .java   data layer, 19 tests
├── bukkit                      91 .java   shared platform core
└── platforms/                  ← container, no build.gradle
    ├── engine                   1 .java   universal entry point + plugin.yml
    ├── foliamc                  5 .java   Folia entry point + 24 resources
    ├── papermc                  3 .java   Paper entry point + 24 resources
    └── spigotmc                 2 .java   Spigot entry point + 24 resources
```

Every package sits under `io.github.mcoriax.mcidentity`, with one subpackage
per module: `api` has `api.model` and `api.db`; `common` has `common.model`,
`common.db`, and `common.central`; `bukkit` has `bukkit.commands`,
`bukkit.event`, `bukkit.listeners`, `bukkit.permission`, `bukkit.scheduler`,
`bukkit.tabcompleter`, and `bukkit.util`; and each platform module keeps its
own.

### Why the container is skipped

Gradle creates an implicit project for every path segment in an
`include` statement. Writing `include 'platforms:foliamc'` in `settings.gradle`
therefore produces a `platforms` project alongside the six real ones — one
Gradle project that has no build file, no sources, and nothing to do.

The root `build.gradle` removes it:

```groovy
subprojects {
    if (!file('build.gradle').exists()) {
        return
    }
    ...
}
```

**This is a silent skip, not an error.** A new module directory without its own
`build.gradle` is skipped without a warning, and the same `return` in the
publishing block is the only thing that decides which modules are published
(see [§5](#5-what-gets-published)). A module can therefore be present on disk,
listed in `settings.gradle`, and still be doing nothing. If a module seems to
have no effect, check that it has a `build.gradle` first.

## 2. Layer order

Dependencies point in one direction only:

```
api  ←  common  ←  bukkit  ←  platforms:{spigotmc, papermc, foliamc}  ←  platforms:engine
```

A module may use the ones to its left. Nothing may use the ones to its right —
in particular `api` and `common` never see a Minecraft type, which is what lets
the data layer be unit tested with no server running.

## 3. Dependency scoping

Scoping is deliberately non-uniform, and the difference is load-bearing rather
than incidental.

| Module | How it sees its siblings | Why |
|---|---|---|
| `api` | — (leaf) | A published library. Applies `java-library`; it depends on nothing. |
| `common` | `compileOnly project(':api')` | Compiles against the contracts, but must not drag `api` into every consumer of the shaded jar. |
| `bukkit` | `api project(':api')`, `api project(':common')` | The **only** module that re-exposes. Its consumers need the contracts on their own compile classpath. |
| `platforms:*` | `compileOnly` on all three | These modules are *bundled into* engine. `implementation` or `api` would carry a second copy of every `api`, `common`, and `bukkit` class into the universal jar. |
| `platforms:engine` | `implementation` on all six | It is the shaded artifact, so it is the one place the classes actually belong. |

### Why the platform modules differ

`spigotmc`, `papermc`, and `foliamc` are not distributed. They exist so the
universal jar can carry every scheduler implementation and pick the right one
at runtime. Each declares its siblings `compileOnly` and ships only its own
classes, so the shading in `platforms:engine` produces exactly one copy of
everything. Promoting any of them to `api` or `implementation` duplicates the
whole core inside the final jar.

The same reasoning covers the database drivers. `org.postgresql:postgresql` and
`org.mongodb:mongodb-driver-sync` are `compileOnly` everywhere they appear and
are downloaded by the server at runtime from the `libraries:` list in each
`plugin.yml`. SQLite and MySQL drivers ship with Spigot and Paper, so only these
two are listed.

### Two consequences worth knowing

* **`compileOnly` does not reach the test classpath.** Every platform module
  adds `testImplementation project(':bukkit')` explicitly, because `bukkit` is
  what re-exposes `api` and `common`. Without that line the platform tests do
  not compile.
* **Only `platforms:engine` compiles against the Folia API.** It is a superset
  that declares the `org.spigotmc:spigot-api` capability, so it provides every
  Bukkit, Paper, and Folia type the bundled schedulers reference. Declaring all
  three platform APIs at once is impossible — they each provide that same
  capability, and Gradle rejects them as a mutually exclusive conflict. The
  individual platform modules compile against their own API (`spigot-api`,
  `paper-api`, `folia-api` respectively); only the engine, which must reference
  `FoliaPlatformScheduler` and `BukkitPlatformScheduler` in one compilation
  unit, needs the union. Platform detection is done by runtime reflection
  (`Class.forName`), so no compile-time Paper or Spigot dependency is required.

## 4. The build graph as a citizen

One test does not merely exist in the build — it is wired into it.

`ArchitectureBoundaryTest` in `common` reads the **source files of the `bukkit`
and `platforms:*` modules** and fails the build when any of them names a storage
internal (`IMCIdentityDB`, a concrete backend, `StatKeys`, or the
`common.db` / `common.central` packages). Those files are not on `common`'s
compile classpath, so they are declared as **inputs of the test task**:

```groovy
test {
    inputs.files(
        fileTree("${rootDir}/bukkit/src/main/java") { include '**/*.java' },
        fileTree("${rootDir}/platforms") { include '*/src/main/java/**/*.java' }
    ).withPropertyName('platformSources').withPathSensitivity(PathSensitivity.RELATIVE)
}
```

Without those inputs Gradle would consider the test up to date after the first
run, and a violation introduced later would never be executed.

**Adding a new platform module means adding a row to the test.** The Gradle
`fileTree` glob already covers any new `platforms/<name>/src/main/java`, so the
task re-runs on its own — but the test's own `PLATFORM_SOURCE_ROOTS` array is
an explicit list, and a module missing from it is silently unchecked.

## 5. What gets published

Only three modules reach GitHub Packages. The gate is a plain `return` in the
root `subprojects` block, placed after the toolchain and before
`apply plugin: 'maven-publish'`:

```groovy
if (project.name != 'api' && project.name != 'common' && project.name != 'bukkit') {
    return
}
apply plugin: 'maven-publish'
```

`api`, `common`, and `bukkit` are the pure-Java libraries a third-party plugin
compiles against. The platform jars are distributed as plugin artifacts and are
never published.

### The artifactId collision this prevents

Left to itself, Gradle's `java` component publishes under the bare project
name. Three modules in the same group would each publish
`io.github.mcoriax:api` — or rather, all three would try, and a consumer could
not depend on the core and an add-on at the same time. The publication block
qualifies every coordinate:

```groovy
artifactId = "${rootProject.name}-${project.name}"
```

so the three published artifacts are `io.github.mcoriax:mcidentity-api`,
`mcidentity-common`, and `mcidentity-bukkit`. This matches the `archivesName`
each module already sets for its jar.

## 6. The universal jar

`platforms:engine` is the only distributed artifact. One jar boots on SpigotMC,
PaperMC, and Folia: `MCIdentityEnginePlugin` probes for Folia's regionised
server class, then Paper's config class, and installs the matching
`PlatformScheduler` at enable time.

All four platform modules disable their thin jar and rely on `shadowJar`:

```groovy
jar { enabled = false }

shadowJar {
    archiveClassifier.set('')
    mergeServiceFiles()
    configurations = [project.configurations.runtimeClasspath]
}
```

`archiveClassifier` is cleared so the artifact is `MCIdentity.jar` rather than
`MCIdentity-all.jar`, and `mergeServiceFiles()` concatenates the `META-INF`
service descriptors instead of letting the first one win.

### One plugin.yml, not four

Each bundled platform jar ships its own `plugin.yml`. Left alone, the shadow
would write four descriptors into one archive and the server would load an
arbitrary one. Engine's rule keeps exactly one:

```groovy
def ownResourcesDir = sourceSets.main.output.resourcesDir.absolutePath

shadowJar {
    filesMatching('plugin.yml') {
        if (!it.file.absolutePath.startsWith(ownResourcesDir)) {
            it.exclude()
        }
    }
}
```

A `plugin.yml` survives only if it came out of *this* module's resources
output; every copy pulled in from a dependency jar is excluded.

### The ordering is explicit, not inferred

The three platform modules disable their thin `jar` task, so the only artifact
Gradle can put on engine's compile classpath is each module's `shadowJar`. The
platforms are wired as explicit `dependsOn` rather than left to task
heuristics:

```groovy
['spigotmc', 'papermc', 'foliamc'].each { platform ->
    tasks.named('compileJava') { dependsOn ":platforms:${platform}:shadowJar" }
    tasks.named('shadowJar') { dependsOn ":platforms:${platform}:shadowJar" }
}
```

Both `compileJava` and `shadowJar` are covered — the first because engine
compiles *against* `FoliaPlatformScheduler`, the second because it bundles it.

### Export

`build.finalizedBy exportJar` copies the finished jar to the root `build/`
directory, and — when the `ENGINE_PLUGIN_JAR_PATH` environment variable names an
existing directory — to a local test server's `plugins/` folder, clearing
previous `mcidentity*.jar` files there first.

## 7. Configuration cache

`gradle.properties` turns on all three:

```properties
org.gradle.configuration-cache=true
org.gradle.parallel=true
org.gradle.caching=true
```

The build is written to keep the configuration cache valid, which constrains how
the build scripts may be written:

* **Close over plain values, not `Project`.** A `FileTree`, a `Configuration`,
  or a task reference captured in a closure is serialized into the cache entry
  and invalidated on the next build. `platforms/engine` captures the resources
  directory as a `String` at configuration time for exactly this reason.
* **Use `java.nio` for file operations inside `doLast`.** `File.copyTo` and
  `File.delete` reach back into Gradle's internals and are rejected under the
  configuration cache. `platforms/engine` uses `java.nio.file.Files.copy` and
  `delete()` instead.
* **Read environment variables at configuration time.** `ENGINE_PLUGIN_JAR_PATH`
  and the publishing credentials are read into locals before the task action, so
  the value is baked into the cached configuration rather than looked up later.

A build script that violates these does not fail — the configuration cache is
discarded and the build gets slower. That is the failure mode to watch for.

Every subproject compiles against a **Java 25** toolchain, provisioned
automatically by the `foojay-resolver` plugin declared in `settings.gradle`.
The wrapper pins Gradle 9.5.0; never invoke a system-installed `gradle`.
