# Changelog — 4.9.0

**Released:** 2026-10-25

Adds an `agents` block to `config.yml` that picks the AI model and OpenRouter key
MCIdentity's agent work runs on. Both default to `global`, which shares whatever
MCAgents itself is configured with, so the owner sets the model and key once for the
whole server. Server owners: nothing to do; existing servers keep today's behaviour.
Requires MCPaimon/mcagents 6.2.0 for the `global` lookup, and falls back on older
MCAgents builds.

## Added

- `agents.model` and `agents.token` in the spigotmc, papermc and foliamc `config.yml`
  copies, both `global` by default (#133).
- `AgentsSettings` and `MCAgentsGlobal` (`bukkit`, package `config`): read the block
  (model lower-cased) and resolve `global` at runtime from MCAgents' `getAgentsModel()`
  / `getAgentsToken()` by reflection, falling back to the provider's
  `getDefaultModel()` / `getServerApiKey()` on MCAgents older than 6.2.0 (#133).
- `AbstractMCIdentityPlugin#getAgentsSettings()`, `getAgentsModel()` and
  `getAgentsToken()`; the plugin logs which model is in use on enable, never the key
  (#133).
- `bukkit` gains JUnit 6.1.0 and `AgentsSettingsTest` (#133).
- `.agents/memory/tasks/agents-config.md`, the task record for this release (#132).

## Changed

- `wiki/environments/env.md` documents the `agents` block and what `global` means;
  `wiki/guides/testing.md` and the agent repository map list the new tests (#133).
