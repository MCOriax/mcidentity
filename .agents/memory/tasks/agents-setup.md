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
