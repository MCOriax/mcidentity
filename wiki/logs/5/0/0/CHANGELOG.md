# Changelog — 5.0.0

**Released:** 2026-11-14

A major release. Identities become wearable professions: a player has one character, keyed
by their own player UUID, and wears one profession at a time. The API, events, commands,
permissions, config and storage all break.

## Changed

- `IdentityProvider#getActiveProfession(UUID)` replaces `getActiveIdentity`; `setActiveProfession` and `clearActiveProfession` change it (#148).
- `MCProfessionEquipEvent` and `MCProfessionUnequipEvent` replace the identity events; `MCIdentityStatAddEvent` carries the player UUID (#148).
- Storage uses `player_stats`, `player_professions`, `player_profession_progress` and `profession_logs` on SQLite, MySQL, PostgreSQL, MongoDB and the central server; `identity_professions` stays (#148).
- Level and EXP are tracked per player and profession; stats and stat points per player, with 100 stat points per level (#148).
- The central client calls the player keyed `/v1/players/...` routes and `/v1/profession-logs` (#148).
- `mcidentity_get_my_identity` and `mcidentity_get_my_stats` report the worn profession and player stats (#148).

## Added

- `/identity wear <profession>` and `/identity unwear`, with a profession GUI (#148).
- `mcoriax.mcidentity.profession.*` (`default: true`) and `mcoriax.mcidentity.profession.<code>` decide who may wear what (#148).
- `profession_swap_cooldown` config key (#148).

## Removed

- Identity UUIDs, identity items, aliases, alternative base identities and the admin identity commands (#148).
- The isolated identity inventory and enderchest; the player keeps the vanilla ones. Player storage belongs to future plugins (#148).
- `identity_swap_cooldown`, `drop_on_death` and the `mcidentity.admin` node (#148).

## Upgrading

No migration: 4.x identity data is not copied. Start from a fresh database, rename
`identity_swap_cooldown` to `profession_swap_cooldown`, and update add-ons to versions built
against MCIdentity 5.0.0.
