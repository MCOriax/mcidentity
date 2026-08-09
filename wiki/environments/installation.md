# Installation & Setup

[← Back to README](../../README.md)

The plugin runs on **SpigotMC**, **PaperMC**, and **Folia** servers. Use the build for your platform and follow the steps below.

1. Place the plugin JAR in your server's `plugins/` folder.
2. Start the server once to generate the default configuration.
3. Pick a storage backend in `config.yml` (see [Data Storage](../reference/database.md)).
   The default is **SQLite**, which needs no setup at all:
   ```yaml
   database:
     type: sqlite
   ```
   MySQL, PostgreSQL, and MongoDB are configured in their sections under
   `database`. To share the MCIdentity central server (the `server-expressjs`
   repository) across a network instead, set `database.type: central` and
   point the plugin at your deployment:
   ```yaml
   database:
     type: central
   central_server:
     url: "https://server.com"
     token: "the value of SERVER_API_TOKEN on the central server"
     timeout_seconds: 10
   ```
4. Restart the server. The plugin initializes the selected storage on startup
   and logs an error if it is unreachable.

## Death behaviour

By default a player keeps their equipped identity when they die and continues
wearing it after respawning. To make the identity key drop on death instead —
sending the player back to the base `player` profession — set:

```yaml
drop_on_death: true
```

`drop_on_death` is `false` by default.

With a shared backend (MySQL, PostgreSQL, MongoDB, or the central server),
every game server on the network uses the same connection settings, so
identities follow players between servers automatically.
