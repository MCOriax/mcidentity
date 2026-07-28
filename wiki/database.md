# Data Storage

[← Back to README](../README.md)

The plugin supports multiple storage backends, selected with `database.type`
in `config.yml`: **SQLite** (the default), **MySQL**, **PostgreSQL**,
**MongoDB**, and the **MCIdentity central server** (the `server-expressjs`
repository). Standalone servers can run entirely on the zero-configuration
SQLite file, while networks can share one MySQL, PostgreSQL, or MongoDB
deployment — or point every node at the central REST server.

## Choosing a backend

| `database.type` | Storage | Best for |
|---|---|---|
| `sqlite` | Local file in the plugin data folder | Single servers; works out of the box with no setup. |
| `mysql` | MySQL or MariaDB server | Networks that already run a MySQL-compatible database. |
| `postgresql` | PostgreSQL server | Networks that prefer PostgreSQL. |
| `mongodb` | MongoDB deployment | Document-oriented storage or existing MongoDB infrastructure. |
| `central` | MCIdentity central REST server | Whole networks sharing one authenticated API with token roles. |

Each backend has its own connection section in `config.yml`:

```yaml
database:
  type: sqlite
  sqlite:
    file: "mcidentity.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "mcidentity"
    username: "root"
    password: "password"
  postgresql:
    host: "localhost"
    port: 5432
    database: "mcidentity"
    username: "postgres"
    password: "password"
  mongodb:
    uri: "mongodb://localhost:27017"
    database: "mcidentity"
```

The `central` type reads the existing `central_server` section (`url`,
`token`, `timeout_seconds`).

## How it works

- All backends implement the same `IMCIdentityDB` contract (in the `api`
  module), so gameplay code never knows which storage is active.
- `SQLiteDB`, `MySQLDB`, and `PostgreSQLDB` (in the `common` module) share
  their query logic through `AbstractSqlDB` and differ only in schema DDL
  and upsert dialect. `MongoDB` stores each identity as one document with
  embedded properties, stats, cosmetics, and equipped slots.
- `MCIdentityCentralServer` implements the same contract by sending
  asynchronous JSON requests over a single pooled HTTP client, carrying the
  bearer token configured in `config.yml` (`central_server.token`).
- Every operation runs asynchronously on a dedicated worker pool, so the
  game thread is never blocked.
- Reads that fail resolve to safe fallbacks (empty results or zero) so the
  game server keeps running; failures are logged.
- Stat identifiers are validated against a fixed allow-list before they are
  used, on the plugin and (for the central server) on the API as well.

## Drivers

The SQLite and MySQL JDBC drivers ship with SpigotMC, PaperMC, and Folia.
The PostgreSQL driver and the MongoDB client are packaged inside the plugin
jar, so no backend requires extra downloads.

## Schema

The relational backends create their tables automatically on first start
(`identity_professions`, `identity_runes`, `identity_keys`,
`identity_properties`, `identity_stats`, `identity_equipped_runes`,
`identity_sessions`, `identity_equipped_cosmetics`, `identity_logs`,
`player_base_slots`). The
SQLite schema matches the central server's, so a database file can be moved
between a game server and a central server deployment.

> **Upgrading from 2.x:** the ability system moved into the standalone
> [MCIdentitySkill](https://github.com/MCOriax/mcidentity-skill) add-on, where a
> skill's data lives on the item rather than in a table. The first start after
> upgrading **drops** `identity_abilities` and `identity_equipped_abilities`
> (and, on MongoDB, the `identity_abilities` collection and the per-identity
> `equipped_abilities` field). Those rows are not recoverable — back the
> database up first if you want to keep them.

## Data stored

| Resource | Contents |
|---|---|
| Professions, runes | The shared registries identities reference. |
| Identity keys | Each identity's UUID and profession. |
| Properties | Level, experience, stat points, textures, and serialized inventory/enderchest storage. |
| Stats | The nine numerical attributes (energy, health, food, strength, defense, wisdom, luck, agility, resilience). |
| Equipped runes | Slot assignments per identity. |
| Cosmetics | Active skin (Base64) and aura (system code) per identity. |
| Sessions | Which identity each player currently wears, with the serialized identity item. |
| Base identity slots | How many alternative base "player" identities each player has unlocked. |
| Logs | The append-only audit trail (EQUIP, UNEQUIP, DEATH_DROP, ADMIN_REMOVE). |

See the central server repository for the REST API reference, environment
configuration, and its storage details.
