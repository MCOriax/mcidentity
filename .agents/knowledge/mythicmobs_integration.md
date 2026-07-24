---
name: mythicmobs-integration
description: Ability-to-skill mapping and NBT skin storage for the optional MythicMobs integration.
---

# Knowledge: MythicMobs Integration

MCIdentity abilities interoperate with MythicMobs skills.

## Ability Mapping

* An `Ability`'s `abilityCode` maps to a MythicMobs skill ID. Triggering an
  equipped ability (`EquippedAbility`) casts the corresponding MythicMobs skill.
* Keep the mapping data-driven — resolve skill IDs from configuration, never
  hardcode MythicMobs internals into core logic.

## Ability Skins

* An ability's `abilitySkin` is stored as `CustomModelData` via persistent NBT
  on the item, so client-side visuals survive relogs and inventory moves.
* Write NBT on the `PlatformScheduler` (item/inventory mutation is main-region
  work under Folia); resolve skin values from cached identity state.

## Soft Dependency

MythicMobs is an optional integration. Guard all MythicMobs access behind a
presence check and degrade gracefully when the plugin is absent — never fail the
game thread if a skill ID cannot be resolved (`ethics/safe_degradation.md`).
