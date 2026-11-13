# Key Features

[← Back to README](../../README.md)

- **Multi-Platform Support**: Fully supports **SpigotMC**, **PaperMC**, and **Folia**.
- **One character per player**: All data is keyed by the player's own UUID. A player has nine stats (energy, health, food, strength, defense, wisdom, luck, agility, resilience) and a pool of unspent stat points. There are no identity UUIDs, identity items, aliases or alternative identities.
- **Profession wearing**: A player wears one profession at a time, with `/identity wear <profession>`, `/identity unwear`, or by clicking it in the profession GUI. A player who wears nothing wears the built-in base `player` profession. Wearing fires the cancellable `MCProfessionEquipEvent`; unwearing fires the cancellable `MCProfessionUnequipEvent`.
- **Per profession progress**: Each profession keeps its own level and EXP for each player, so wearing a profession again resumes where the player left it. Every level gained in any profession grants 100 stat points to the player.
- **Wearing permissions**: A player may wear a profession when they hold `mcoriax.mcidentity.profession.*` (granted to everyone by default) or `mcoriax.mcidentity.profession.<code>`. Revoke the wildcard and grant single nodes to limit which professions a group may wear. The base `player` profession is always allowed.
- **Swap cooldown**: `profession_swap_cooldown` in `config.yml` sets the minutes a player waits between two profession changes (0 disables it).
- **Vanilla inventory always**: MCIdentity never saves, swaps or drops items. The inventory and enderchest stay vanilla; player storage belongs to the planned MCMultiverse and MCRealm plugins. Death keeps the worn profession and restores current health, food and energy for the respawn.
- **Base "Player" Profession**: The built-in `player` profession is hardcoded in memory. It does **not** generate a `professions/player.yml` file and cannot be changed through YAML.
- **Passive Base Leveling**: Every online player gains 1 EXP per second on the base `player` profession, whatever they wear, at 86,400 EXP per level (one level per day of play time) with no cap. Gains are buffered in memory and flushed in bulk every five minutes and on quit.
- **Profession name tags**: The worn profession is shown before the player's name in chat and in the tab list, read from the RAM session so nothing waits on the database.
- **Skills, runes, and cosmetics as separate add-ons**: [MCIdentitySkill](https://github.com/MCOriax/mcidentity-skill), [MCIdentityRune](https://github.com/MCOriax/mcidentity-rune) and [MCIdentityCosmetic](https://github.com/MCOriax/mcidentity-cosmetic) each own their storage, keyed by the player UUID, and read the worn profession through `getActiveProfession`.
- **Regionalized Threading**: Fully compatible with Folia's `RegionScheduler` and `GlobalRegionScheduler`.
- **Multi-Database Storage**: SQLite (the zero-configuration default), MySQL, PostgreSQL, or MongoDB, selected with `database.type` in `config.yml`. All backends run asynchronously behind the same `IMCIdentityDB` contract.
- **Central Server Storage**: Alternatively, all profession data can live on the MCIdentity central server, accessed through an authenticated REST API over a pooled HTTP client, so every game server on the network shares one data set.
- **Cold-Boot Login Protection**: With the `central` backend, the plugin pings the central server's `/health` endpoint during the asynchronous pre-login phase and refuses players with *"The database is starting up. Please come back in 1 minute."* while storage is not ready. Initialization retries every 10 seconds until it succeeds.
- **Extensible API**: `MCIdentityProvider` lets third-party plugins read the worn profession, progress and stats, and wear professions for a player.
