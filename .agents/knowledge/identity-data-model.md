---
name: identity-data-model
description: One character per player keyed by the player UUID, the worn profession, per profession progress, and the RAM session cache.
---

# Knowledge: Identity Data Model

Since 5.0.0 MCIdentity is a profession manager. There are no identity UUIDs,
identity items, aliases or alternative identities, and nothing is migrated from 4.x.

## The character

* One character per player, keyed by the player's own UUID.
* `player_stats` holds the unspent stat points and the nine stats. Stats belong to the
  player, not to a profession.
* `player_professions` holds the worn profession. No row means the base `player`
  profession (`BaseProfession`, hardcoded, never configurable through YAML).
* `player_profession_progress` holds level and EXP per (player, profession). A
  profession never worn starts at level 1 (`ProfessionProgressRecord.starting()`).
* Every level of any profession grants `BaseProfession.STAT_POINTS_PER_LEVEL` stat
  points. Passive EXP goes to the base profession only.

## Wearing

`bukkit/profession/ProfessionWearing` is the single entry point: profession node
(`mcoriax.mcidentity.profession.*` or `.<code>`), swap cooldown, pending flag, the
cancellable `MCProfessionEquipEvent` / `MCProfessionUnequipEvent`, then
`MCIdentityProvider.setActiveProfession` and `logAction`. The inventory is never
touched; player storage belongs to the planned MCMultiverse and MCRealm plugins.

## Caching Layer

* `ActiveProfessionSession` (inside `MCIdentityManager`) holds the online player's
  worn profession, its progress, the stats and the live health, energy and food.
* `MCIdentityManager.getActiveProfession(UUID)` is the synchronous RAM read add-ons
  use.
* Updates apply to the session first for instant feedback, then flush to storage
  asynchronously. See `performance/ram-caching-rules.md`.
