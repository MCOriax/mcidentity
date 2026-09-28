---
name: agents-setup
description: Record of the six-task agent setup — the Gradle project-structure doc, the agent orientation page, the repository rules refresh, and the memory state seed.
status: in-progress
---

# Task: Agents Setup

## Goal

Give this repository a documented build, a documented orientation page, and a
seeded memory, without touching a line of Java and without bumping the version.

Six tasks, one branch each, stacked. Task 1 is this record; task 6 is the
release. Nothing is pushed and no pull request is opened by this work — the
branches are local, so the `PR` column below is deliberately empty.

## Mode

**Consumer.** The shared instruction set is resolved through the
`lxagents-agents-base` connector and is never vendored into this repository.
The repository contributes only what is its own: build rules, module boundaries,
and orientation.

## Tasks

| # | Title | Branch | Files / areas | PR |
|---|---|---|---|---|
| 1 | Task record | `chore/agents-setup-plan` | `.agents/memory/tasks/agents-setup.md`, `.agents/INDEX.md` | |
| 2 | Gradle project-structure doc (human) | `docs/gradle-project-structure` | `wiki/information/project-structure.md`, `wiki/information/architecture.md`, `wiki/INDEX.md` | |
| 3 | Agent orientation page | `docs/agent-repository-map` | `.agents/wiki/context/repository-map.md`, `.agents/INDEX.md`, `AGENTS.md` | |
| 4 | Repository rules refresh | `docs/gradle-module-rules` | `.agents/rules/repository.md`, `.agents/dependencies/gradle-multi-project-rules.md` | |
| 5 | Memory state seed | `chore/memory-state-seed` | `.agents/memory/state/repository-state.md` | |
| 6 | Release | `chore/agents-setup-release` | `tasks/agents-setup.md`, `wiki/logs/4/6/1/CHANGELOG.md` | |

## Decisions

**D1 — The build is documented once, for humans, in `wiki/`.** The Gradle
structure, the layer order, the dependency-scoping table, the publishing gate,
the universal jar, and the configuration-cache constraints have no home today.
`wiki/information/architecture.md` has a `## 2. Modules` section but treats
`platforms` as one block and never explains the build. Task 2 adds a dedicated
page and cross-links it; the agent-facing page in task 3 points at it rather
than restating it.

**D2 — The `INDEX.md` migration is out of scope.** The current shared set forbids
the filename `INDEX.md` anywhere and wants indexes at
`.agents/index/{scope}-index.md`. This repository still has four of them
(`INDEX.md`, `.agents/INDEX.md`, `wiki/INDEX.md`, `wiki/logs/INDEX.md`), and
`AGENTS.md:36-41` already admits it. Migrating them is a multi-file link refactor
and a separate piece of work. This record does not attempt it, and every index
this task touches is updated in its existing convention.

**D3 — No new version directory.** The project stays at `4.6.1`, and
`wiki/logs/4/6/0/` is not created. Anything versioned goes into the existing
`wiki/logs/4/6/1/CHANGELOG.md`. Creating a version directory *is* a version
claim, and there is no release to claim.

**D4 — `LICENSE` stays the `# License` placeholder.** Inventing a license type, a
holder, or a year is a legal statement, and there are none to infer.

**D5 — No pull requests.** The gates stand: ask before opening a pull request,
ask before merging. The branches stay local and the `PR` column stays empty. Do
not fill it with placeholder numbers.

**D6 — Documentation only.** No Java source, no `build.gradle`, and nothing under
`.gradle/` or `build/` is touched. The build is read, never run, by this work.

## Entries

### Task 1 — Task record (2026-09-28)

**Branch:** `chore/agents-setup-plan` · **PR:** none — not pushed

Wrote this record, and registered it in `.agents/INDEX.md` in the same commit.
The investigation behind the decisions above read `settings.gradle`, the root
`build.gradle`, `gradle.properties`, all seven subproject build files, and the
`ArchitectureBoundaryTest` source; nothing was written outside `.agents/`.

**Open at this point:** every task from 2 to 6, plus the `INDEX.md` migration
(D2), which this work explicitly does not attempt.

### Task 2 — Gradle project-structure doc (2026-09-28)

**Branch:** `docs/gradle-project-structure` · **PR:** none — not pushed

Added `wiki/information/project-structure.md`, the document that did not exist:
seven projects and the skipped `platforms/` container, the one-direction layer
order, the scoping table with the reason each row differs, the publishing gate
and the `artifactId` collision it prevents, the universal jar with its
`plugin.yml` dedup rule and explicit `dependsOn` wiring, the
`ArchitectureBoundaryTest` task inputs, and the configuration-cache
constraints. Cross-linked it from `wiki/information/architecture.md` — both in
the introduction and under `### platforms`, which previously described the
container as if it were a single module — and added the row to `wiki/INDEX.md`,
all in one commit.

**One correction to the investigation.** The claim was that adding a platform
module means extending the Gradle `fileTree`. The `fileTree` glob
(`platforms/*/src/main/java/**/*.java`) already covers any new module, so the
task re-runs on its own. The hardcoded list is the test's own
`PLATFORM_SOURCE_ROOTS` array in `ArchitectureBoundaryTest`, and a module
missing from it is silently *unchecked*, not silently unchecked-and-passing.
The document says so.

**Also corrected:** "only engine compiles against the Folia API" is true of the
*union* requirement, but `platforms:foliamc` necessarily compiles against it
too. The real constraint is that engine is the only module needing every
platform type in one compilation unit, and the only one for which declaring all
three APIs would be a mutually exclusive Gradle capability conflict.

**Open at this point:** tasks 3 to 6, plus D2.

### Task 3 — Agent orientation page (2026-09-28)

**Branch:** `docs/agent-repository-map` · **PR:** none — not pushed

Added `.agents/wiki/context/repository-map.md` and registered it in
`.agents/INDEX.md` under a new `## Wiki` section, in the same commit. The page
carries what lives where, the build/test/run/export/publish commands with what
each actually does, the five entry points and the role of each, the generated
paths to leave alone, and the build traps.

The traps are the part worth keeping. Each one is a way this build fails
*quietly* rather than loudly: a module with no `build.gradle` is skipped without
a warning; a new platform module makes `ArchitectureBoundaryTest` go green
while quietly not reading the new sources, because the Gradle `fileTree` glob
picks them up but the test's own `PLATFORM_SOURCE_ROOTS` array does not; and a
build script that breaks configuration-cache compatibility does not fail, it
just makes every build slower.

**The false note in `AGENTS.md` is gone.** It claimed `.agents/wiki/` did not
exist in this repository and pointed orientation at `wiki/information/` instead.
It now points at the new page, and the row above it in the same table already
referenced a path that did not exist — that row now resolves.

**Found while reading:** there is no `.github/` directory. The
`workflows/ci-pipeline.md` file describes a pipeline this project expects but
nothing in the working tree runs it, so the orientation page says so rather
than letting an agent cite it as evidence a check passed.

**Open at this point:** tasks 4 to 6, plus D2.

### Task 4 — Repository rules refresh (2026-09-28)

**Branch:** `docs/gradle-module-rules` · **PR:** none — not pushed

Rewrote `.agents/dependencies/gradle-multi-project-rules.md` around the
constraints a change must not break, and added a routing table for the scoping
question. The new sections: a module with no `build.gradle` is skipped
silently; the `compileOnly` rule covers the *shared* modules, not just platform
APIs; `compileOnly` does not reach the test classpath, which is why each
platform module re-adds `testImplementation project(':bukkit')`; the publishing
gate and why the `artifactId` qualification is load-bearing; and the
`ArchitectureBoundaryTest` inputs, with the note that a new platform module
leaves the test green while it stops reading the new sources.

`rules/repository.md` gained the verified command table (`exportJar`, and the
fact that root `clean` touches only the root `build/`), the test-coverage map,
and the two quiet boundary-enforcement traps. Its module-boundary bullet links
`module-isolation.md` for the rule rather than restating it, and adds only the
build facts that file does not carry.

**One false claim corrected.** The old shading section said to "keep relocations
consistent so shaded dependencies never clash with other plugins on the server".
There is no `relocate` call anywhere in the build — the shadow is a plain
`runtimeClasspath` merge, and every bundled class keeps its original
coordinates. That is correct today, because everything bundled is part of this
plugin, but the sentence described a protection that does not exist. It now
says so, and says what would have to change if a shared library were ever
shaded.

Both index rows updated in the same commit. No shared rule was restated; the
fresh material is repository facts, and the shared conventions stay addressed
through `{shared}`.

**Open at this point:** tasks 5 and 6, plus D2.

### Task 5 — Memory state seed (2026-09-28)

**Branch:** `chore/memory-state-seed` · **PR:** none — not pushed

Added `.agents/memory/state/repository-state.md` and registered it in
`.agents/INDEX.md` in the same commit, under `## Memory`, ahead of the two task
records. It is written to be overwritten in place — a state file describes the
present, so a later session replaces the body rather than appending a second
dated section, and the file says so.

It carries the version and identity table, the stack, what is built, what is
not, the branch-by-branch state of this work, and the next obvious step. It
points at `repository-map.md` for orientation and at the rules file for rules
rather than restating either, and it opens by saying that the repository wins
where the two disagree.

**Four things surfaced while writing it, none of which any existing document
recorded.**

* There is no `v4.6.1` tag. The newest tag is `v4.6.0`, against a
  `wiki/logs/4/6/1/CHANGELOG.md` dated 2026-08-27. A previous release's changelog
  shipped without its tag; nothing in the repository claims this is intentional.
* Nothing in this repository has ever run CI. There is no `.github/`, so the
  4.6.1 release claims "two assertion-free tests" and a SonarCloud sweep with
  nothing in the tree that would have caught a regression.
* `origin/master` is four commits behind `master`; this stack is local only.
* The `403 Key limit exceeded` failure recorded in the original investigation is
  still unexplained, so it is carried here as an open decision rather than
  written off.

**A conflict worth naming.** The shared set registers memory in
`.agents/index/memory-index.md`, which does not exist here. D2 puts the
`INDEX.md` migration out of scope, and this repository's own `AGENTS.md` and
`INDEX.md` make `.agents/INDEX.md` the sole authority over `.agents/`. So the
row went into `.agents/INDEX.md`. The shared rule is not overridden — nothing
declares an override — it simply has nowhere to land until D2 is done, and
creating `memory-index.md` early would have meant creating one piece of the
migration D2 defers.

**Open at this point:** task 6, plus D2.
