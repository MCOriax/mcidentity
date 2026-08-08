---
name: wiki-index
description: Index of wiki/ — project documentation for the MCIdentity plugin.
---

# Wiki Index

**Scope:** `wiki/`
**Parent:** [root-index](../INDEX.md)

Any page added to or removed from `wiki/` is reflected here in the same commit.
This index never writes into `.agents/`.

## Information

| File | Purpose |
|---|---|
| [`information/overview.md`](information/overview.md) | What the plugin is, its scope boundary, and where the add-ons live. |
| [`information/features.md`](information/features.md) | Feature-by-feature description of what the plugin offers. |
| [`information/architecture.md`](information/architecture.md) | Gradle modules, dependency rules, and the storage abstraction. |

## Environments

| File | Purpose |
|---|---|
| [`environments/requirements.md`](environments/requirements.md) | Runtime, build, and functional requirements. |
| [`environments/installation.md`](environments/installation.md) | Installing the plugin and connecting it to the central server. |
| [`environments/env.md`](environments/env.md) | Runtime configuration keys and environment variables. |

## Reference

| File | Purpose |
|---|---|
| [`reference/commands.md`](reference/commands.md) | `/identity` subcommands and their permission nodes. |
| [`reference/database.md`](reference/database.md) | Storage backends and how to configure each one. |
| [`reference/api.md`](reference/api.md) | Integrating a third-party plugin with `MCIdentityProvider`. |
| [`reference/bukkit-events.md`](reference/bukkit-events.md) | Custom `MCIdentity*Event` types fired for other plugins. |

## Guides

| File | Purpose |
|---|---|
| [`guides/gui.md`](guides/gui.md) | The multi-tier interactive menu system. |
| [`guides/custom-exp.md`](guides/custom-exp.md) | Defining action-based profession EXP rules in YAML. |
| [`guides/testing.md`](guides/testing.md) | Running the unit test suite. |

## Child Indexes

| Index | Scope | Load when |
|---|---|---|
| [`logs/INDEX.md`](logs/INDEX.md) | Versioned change logs | You need release history or must record a change. |
