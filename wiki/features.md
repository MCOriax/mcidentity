# Key Features

[← Back to README](../README.md)

- **Multi-Platform Support**: Fully supports **SpigotMC**, **PaperMC**, and **Folia**.
- **Interactive GUI System**: Multi-tier virtual menus for managing identities, runes, and abilities.
- **Dynamic Data Binding**: Ability and Rune GUIs fetch real-time data from the database, showing equipped items or placeholders.
- **Regionalized Threading**: Fully compatible with Folia's `RegionScheduler` and `GlobalRegionScheduler`.
- **Central Server Storage**: All identity data lives on the MCIdentity central server, accessed through an authenticated REST API over a pooled HTTP client, so every game server on the network shares one consistent data set.
- **Extensible API**: Provides a central `MCIdentityProvider` for third-party plugins to interact with the identity system.
- **Secure Commands**: Role-based permissions for administrative actions.
- **Visual Identity Keys**: Identities are represented as physical items in the game with custom colored display names.
- **Unbound Identities**: Identity items created via `/identity get` are initially "unbound". They lack a unique ID and database entry. Both the unique ID and the identity's initial stats are generated and registered in the database only when the item is first used.
- **Base "Player" Profession**: Every player permanently owns a built-in `player` profession, created automatically in the database on first join and keyed by the player's own UUID. It is the fallback state whenever an identity is unequipped, dropped on death, or fails to equip, so a player is never without a profession. It can never be deleted or turned into an identity key item.
- **Passive Base Leveling**: The base profession gains 1 EXP per second for every online player — even while another identity is equipped — and levels without limit at 86,400 EXP per level (one level per day of play time). Gains are buffered in memory and flushed to the central server in bulk every five minutes and on quit.
- **Base Profession Restrictions**: The base profession may equip skins and auras but is blocked from using runes and abilities.
- **Always-On Storage Isolation**: Each identity's inventory and enderchest are always isolated and stored with the identity; the previous `features.inventory` / `features.enderchest` config toggles have been removed.
