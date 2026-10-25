---
name: memory-task-plugin-unification
description: Record of the cross-plugin unification — shared Publish/Release workflows from MCEngine/actions, Minecraft 26.3 support, a tools/ sub-project for MCAgents, the release-branch publish gate, and release 4.8.0.
---

# Task — Plugin Unification

*Opened on project version 4.7.0. The version is unchanged by this work.*

*Closed on version 4.8.0. The owner later approved a release, so Task 6 cuts 4.8.0 and
ships the whole chain.*

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
  *(Note: the owner later approved the release; it is Task 6.)*
* The same branch names are used in every plugin repository; `MCEngine/actions` merges
  first, and the tools task waits for `MCPaimon/mcagents`.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/plugin-unification-plan` | #125 |
| 2 | Shared Publish and Release callers | `ci/release-workflows` | #126, #127 |
| 3 | Minecraft 26.3 support | `build/minecraft-26-3` | #128 |
| 4 | MCAgents tools sub-project | `feat/agent-tools` | #129 |
| 5 | Publish only after a release branch merges | `ci/release-branch-gate` | #130 |
| 6 | Release 4.8.0 | `release/4.8.0` | #131 |

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

### Task 3 — build/minecraft-26-3

The spigot, paper and folia APIs moved to the 26.3 line (spigot `26.3-R0.1-SNAPSHOT`,
paper `26.3.build`, folia `26.3.build.5-alpha`, because Folia has no stable 26.3 build
yet). The engine layout already matched MCIdentity, so nothing structural changed.
`./gradlew build` passes locally.

* `org.jetbrains:annotations` is now declared `compileOnly` in `bukkit`, because spigot-api 26.3 no longer puts it on the compile classpath.

### Task 4 — feat/agent-tools

Added a pure Java `tools` module (`io.github.mcoriax.mcidentity.tools`) with two read-only,
player-only MCAgents tools: `mcidentity_get_my_identity` (profession, alias, level, EXP, stat
points) and `mcidentity_get_my_stats` (the nine stats plus live health, energy, food). Both read
the RAM session cache only and refuse any caller that is not an online player.

* Bound to MCAgents by reflection: `MCAgentsToolAdapter` proxies `PluginTool` as
  `io.github.mcpaimon.mcagents.tools.AITool`; `bukkit/bridge/mcagents/MCAgentsToolRegistrar`
  registers them at the end of `onEnable`. Absent MCAgents logs one info line.
* `bukkit` takes `tools` as `compileOnly` and `platforms:engine` bundles it with
  `implementation`, so the published `mcidentity-bukkit` does not name an unpublished module.
  `tools` is not published.
* `softdepend: [MCAgents]` added to all four `plugin.yml` files.
* Left: `.agents/` instruction files still describe seven projects (see the task report).

### Task 5 — ci/release-branch-gate

Publish now triggers on a merged pull request from a `release/{version}` branch instead of
every push to `master`, and the shared workflow fails the run when `{version}` is not the
merged `project-version`; `workflow_dispatch` on `master` stays as a retry. Release follows
Publish runs on `release/**` and `master`. The logic lives in `MCEngine/actions`
(MCEngine/actions#3); the callers here only changed their triggers.

### Task 6 — release/4.8.0

Cut version 4.8.0 with the owner's approval: `project-version` bumped, and
`wiki/logs/4/8/0/CHANGELOG.md` covers Tasks 2–5 plus the Publish workflow from #122/#123,
which shipped with tag `v4.7.0` but is not in the 4.7.0 log. The branch stacks on
`ci/release-branch-gate`, so merging it is the first release published by the new gate.
