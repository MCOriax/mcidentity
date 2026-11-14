---
name: profession-wearing
description: Task record for turning identities into wearable professions; one character per player keyed by the player UUID, released as 5.0.0.
---

# Profession wearing

**Status:** done

*Opened on project version 4.12.0; closed on 5.0.0.*

## Goal

A player has one character, keyed by their own player UUID, and wears one profession at a
time. Identity UUIDs, identity items, aliases, alternative base identities and the isolated
identity inventory and enderchest are removed. Every plugin in the family binds personal
data to `("player", <player UUID>)`.

## Objective

* No class in `api`, `common`, `bukkit` or `tools` generates, stores or reads an identity
  UUID.
* `IdentityProvider#getActiveProfession(UUID)` is the read every add-on uses;
  `setActiveProfession` and `clearActiveProfession` change it.
* `MCProfessionEquipEvent` and `MCProfessionUnequipEvent` replace the identity events.
* The player keeps the vanilla inventory and enderchest at all times.
* Storage: `player_stats`, `player_professions`, `player_profession_progress`,
  `profession_logs` on SQLite, MySQL, PostgreSQL, MongoDB and the central server;
  `identity_professions` stays.
* No migration and no legacy support: nothing is in production yet.
* Publish and Release are green on 5.0.0.

## Decisions taken while the owner was away

| Question | Choice |
|---|---|
| Progression | Level and EXP per (player, profession); stats and stat points per player; each level grants 100 stat points. |
| Who may wear what | `mcoriax.mcidentity.profession.*` (default true) or `mcoriax.mcidentity.profession.<code>`. |
| Migration | None (owner, 2026-11-14): not in production, so no copy, no `legacy_*` tables, no old config key or `mcidentity.admin` fallback. |
| Player storage | Never in MCIdentity (owner, 2026-11-14); the planned MCMultiverse and MCRealm own it. |
| Profession file stats | The `stats` section of a profession file no longer applies; a new player starts from the base profession's defaults. |
| Passive EXP | One always on track at 86,400 EXP per level (the old primary track). |
| Version | `5.0.0`: the API, events, commands and storage all break. |

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Record the plan | `plan/profession-wearing` | #147 |
| 2 | Turn identities into wearable professions | `refactor/profession-wearing` | #148 |
| 3 | Release 5.0.0 | `release/5.0.0` | #149 |

## Progress

### Task 1 — plan/profession-wearing

Record opened.

### Task 2 — refactor/profession-wearing

API, models, every storage backend, the central client, the bukkit layer (wearing
service, commands, GUIs, listeners, events), the MCAgents tools, plugin.yml, config,
tests and docs rewritten. Compiled locally with JDK 21 against spigot-api 1.21.10; the
common, tools and bukkit tests pass (103).

### Task 3 — release/5.0.0

project-version set to 5.0.0, changelog and logs index rows added.
