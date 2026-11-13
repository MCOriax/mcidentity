# Data Storage

[← Back to README](../../README.md)

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
  and upsert dialect. `MongoDB` keeps one collection per table, keyed by
  the player UUID.
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

Every backend creates its storage automatically on first start. All player data is
keyed by the player's own UUID.

| Table / collection | Key | Contents |
|---|---|---|
| `identity_professions` | `system_code` | The registry of professions. |
| `player_stats` | `player_uuid` | Unspent stat points and the nine stats (energy, health, food, strength, defense, wisdom, luck, agility, resilience). |
| `player_professions` | `player_uuid` | The profession the player wears and when it was put on. A player with no row wears the base `player` profession. |
| `player_profession_progress` | `player_uuid`, `profession_code` | Level and EXP of each profession the player has worn. |
| `profession_logs` | auto id | The append-only audit trail of `EQUIP` and `UNEQUIP` actions. |

The SQLite schema matches the central server's, so a database file can be moved
between a game server and a central server deployment.

MCIdentity stores no inventory or enderchest.

> **Upgrading from 4.x:** 5.0.0 has no migration. The plugins were not in production,
> so a 4.x database is not read: start 5.0.0 on a fresh database (or drop the old
> `identity_*` and `player_base_slots` tables yourself).

See the central server repository for the REST API reference, environment
configuration, and its storage details.
