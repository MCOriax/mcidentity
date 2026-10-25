---
name: memory-task-agents-config
description: Record of the agents block (model and token, global by default) that resolves against MCAgents' own agents.model and agents.token.
---

# Task — Agents Config

*Opened on project version 4.8.0.*

**Status:** open — task 1 of 3

## Goal

MCIdentity's agent work runs on a model and an OpenRouter key chosen in its own
`config.yml`, and by default shares whatever MCAgents is configured with, so the owner
sets the model and key once for the whole server.

## Objective

* `config.yml` (the spigotmc, papermc and foliamc copies) carries `agents.model: global`
  and `agents.token: global`.
* `AgentsSettings` reads the block; a `global` value resolves at runtime from MCAgents'
  `getAgentsModel()` / `getAgentsToken()` (MCAgents 6.2.0) by reflection, with no compile
  dependency on MCAgents.
* `AbstractMCIdentityPlugin` exposes `getAgentsSettings()`, `getAgentsModel()` and
  `getAgentsToken()`, and logs which model is in use on enable — never the key.
* `./gradlew build` passes with the new `bukkit` tests; the chain merges in order and
  Publish/Release are green.

## Detail and constraints

* MCIdentity sends no prompts to MCAgents — it only registers read-only tools — so no
  call site changes model or key; the getters are there for the tools and the add-ons.
* MCPaimon/mcagents 6.2.0 (already released) provides the getters `global` resolves
  against.

## Decisions taken while the owner was away

| Question | Choice |
|---|---|
| What `global` resolves against | MCAgents 6.2.0's public `getAgentsModel()` / `getAgentsToken()` on its plugin class, looked up on every call. |
| MCAgents older than 6.2.0 | Fall back to the provider's `getDefaultModel()` / `getServerApiKey()`, which give the same values. |
| A non-global value | Used as written; the model is lower-cased (`{platform}/{model}`). |
| Where the settings live | A field on `AbstractMCIdentityPlugin`, read on enable; the plugin has no reload path. |
| Version | `4.9.0`: a new configuration block, no break. |

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Task record | `chore/agents-config-plan` | |
| 2 | Agents block with global fallback | `feat/agents-config` | |
| 3 | Release | `release/4.9.0` | |

## Entries

### Task 1 — chore/agents-config-plan

Record created before any of the work, with the task list above, and its row added to
`.agents/INDEX.md`. Nothing else changed.
Next: task 2 adds the `agents` block and its resolution.
