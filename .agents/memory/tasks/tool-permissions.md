---
name: tool-permissions
description: Task record for giving every MCIdentity AI tool a permission node that MCAgents 6.9.0 filters a player's tool list by, released as 4.12.0.
---

# Tool permissions

**Status:** in progress

*Opened on project version 4.11.1.*

## Goal

A player never spends tokens on MCIdentity tool definitions they cannot use. MCAgents 6.9.0
offers a player only the tools they are OP for or hold the node of, and checks the node
again when a tool runs; this plugin names the node of each of its tools.

## Objective

* `PluginTool#getPermission()` exists, defaulting to `mcoriax.mcidentity.tool.<tool_name>`, and the
  MCAgents adapter answers `getPermission` with it.
* Tools that already guard their action with a node return that node instead.
* Every node is declared in the engine, spigotmc, papermc and foliamc `plugin.yml`.
* Older MCAgents versions keep working: they never ask for the node.
* `./gradlew build` passes; Publish and Release are green on 4.12.0.

## Nodes

| Tool | Node | Default |
|---|---|---|
| `mcidentity_get_my_identity` | `mcoriax.mcidentity.tool.mcidentity_get_my_identity` | `true` |
| `mcidentity_get_my_stats` | `mcoriax.mcidentity.tool.mcidentity_get_my_stats` | `true` |

## Decisions taken while the owner was away

| Question | Choice |
|---|---|
| Node names | `mcoriax.mcidentity.tool.<tool_name>` for tools every player may use; an existing node for a tool that already had one. |
| Version | `4.12.0`: new nodes and a new default method, nothing breaks. |

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Record the plan | `plan/tool-permissions` | |
| 2 | Name a permission node for every tool | `feat/tool-permissions` | |
| 3 | Release 4.12.0 | `release/4.12.0` | |

## Progress

### Task 1 — plan/tool-permissions

Record opened. Nothing built yet.
