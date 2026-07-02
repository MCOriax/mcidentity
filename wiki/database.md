# Data Storage

[← Back to README](../README.md)

The plugin does not connect to a database. All identity data is owned by the
**MCIdentity central server** (the `server-expressjs` repository), and every
game server on the network reads and writes it through an authenticated REST
API. This keeps data consistent across SpigotMC, PaperMC, and Folia nodes and
removes database drivers, credentials, and connection pools from the plugin.

## How it works

- `MCIdentityCentralServer` (in the `common` module) implements the
  `IMCIdentityDB` contract by sending asynchronous JSON requests over a
  single pooled HTTP client.
- Every request carries the bearer token configured in `config.yml`
  (`central_server.token`), matching the central server's read-write
  credential.
- Reads that fail resolve to safe fallbacks (empty results or zero) so the
  game server keeps running; failures are logged.
- Stat identifiers are validated against a fixed allow-list on both the
  plugin and the server before they are used.

## Data owned by the central server

| Resource | Contents |
|---|---|
| Professions, abilities, runes | The shared registries identities reference. |
| Identity keys | Each identity's UUID and profession. |
| Properties | Level, experience, stat points, textures, and serialized inventory/enderchest storage. |
| Stats | The nine numerical attributes (energy, health, food, strength, defense, wisdom, luck, agility, resilience). |
| Equipped abilities and runes | Slot assignments per identity. |
| Cosmetics | Active skin (Base64) and aura (system code) per identity. |
| Sessions | Which identity each player currently wears, with the serialized identity item. |
| Logs | The append-only audit trail (EQUIP, UNEQUIP, DEATH_DROP, ADMIN_REMOVE). |

See the central server repository for the REST API reference, environment
configuration, and storage details.
