---
name: memory-task-plugin-unification
description: Record of the cross-plugin unification — shared Publish/Release workflows from MCEngine/actions, Minecraft 26.3 support, and a tools/ sub-project for MCAgents.
---

# Task — Plugin Unification

*Opened on project version 4.7.0. The version is unchanged by this work.*

## Goal

Every MCEngine-family plugin releases the same way, builds against the latest Minecraft
release, and exposes its features to MCAgents as AI tools — without each repository
carrying its own copy of the logic.

## Objective

* `.github/workflows/publish.yml` and `release.yml` exist here and only call the shared
  workflows in `MCEngine/actions`.
* The platform modules compile against the Minecraft 26.3 server APIs, and
  `./gradlew build` passes.
* A `tools/` sub-project at the repository root provides MCAgents `AITool`s, following
  `MCPaimon/mcagents/TOOL_CREATOR.md`.

## Detail and constraints

* The `platforms/engine` universal-jar architecture from MCIdentity is already in place
  here (engine bundles spigotmc, papermc and foliamc and picks the scheduler at runtime),
  so no structural change is made to it.
* `project-version` is not bumped and no `wiki/logs/` directory is created, so there is
  no release task in this chain.
* The same branch names are used in every plugin repository; `MCEngine/actions` merges
  first, and the tools task waits for `MCPaimon/mcagents`.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/plugin-unification-plan` | |
| 2 | Shared Publish and Release callers | `ci/release-workflows` | |
| 3 | Minecraft 26.3 support | `build/minecraft-26-3` | |
| 4 | MCAgents tools sub-project | `feat/agent-tools` | |

## Entries

### Task 1 — chore/plugin-unification-plan

Record created before any of the work, with the task list above. Nothing else changed.

### Task 2 — ci/release-workflows

Added `.github/workflows/publish.yml` and `release.yml` as thin callers of
`MCEngine/actions` (merged there first). Release attaches `MCIdentity-{version}.jar` from
`platforms/engine`.
The next task depends on nothing from this one.

### Task 2 follow-up — fix/release-package-access

The shared workflows now pass the package token to every Gradle step
(MCEngine/actions#2), and their Release build asks for `packages: read`, so the
Release caller grants it. Without it the run failed to start.
