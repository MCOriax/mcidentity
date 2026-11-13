---
name: profession-wearing
description: Task record for turning identities into wearable professions; one character per player keyed by the player UUID, released as 5.0.0.
---

# Profession wearing

**Status:** in progress

*Opened on project version 4.12.0.*

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
* Storage: `player_professions`, `player_properties`, `player_stats`, `profession_logs` on
  SQLite, MySQL, PostgreSQL, MongoDB and the central server; `identity_professions` stays.
* The first 5.0.0 start migrates each player's primary identity and worn profession, and
  renames the 4.x tables to `legacy_*` instead of deleting them.
* Publish and Release are green on 5.0.0.

## Decisions taken while the owner was away

| Question | Choice |
|---|---|
| Progression | One level, exp, stat point and stat set per player; the profession worn decides which actions earn EXP. |
| Who may wear what | `mcidentity.profession.*` (default true) or `mcidentity.profession.<code>`. |
| Migration | Copy the primary identity (already keyed by the player UUID) and the worn profession; rename old tables to `legacy_*`. |
| Passive EXP | One always on track at 86,400 EXP per level (the old primary track). |
| Version | `5.0.0`: the API, events, commands and storage all break. |

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Record the plan | `plan/profession-wearing` | |
| 2 | Turn identities into wearable professions | `refactor/profession-wearing` | |
| 3 | Release 5.0.0 | `release/5.0.0` | |

## Progress

### Task 1 — plan/profession-wearing

Record opened. Nothing built yet.
