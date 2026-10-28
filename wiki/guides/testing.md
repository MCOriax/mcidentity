# Testing

[← Back to README](../../README.md)

The plugin includes a suite of unit tests for the `common` module to ensure data integrity and logic correctness.

## Running Tests
To run the unit tests, use the following Gradle command:
```bash
./gradlew :common:test
```
The tests cover:
- Core Managers (`MCIdentityManager`, `MCIdentityProvider`)
- The central server client (`MCIdentityCentralServer`), exercised against a stub HTTP server
- Data serialization and session management logic.

The `bukkit` module has its own suite for the `agents` configuration block
(`AgentsSettingsTest`): how `agents.model` and `agents.token` are read and how
`global` resolves against MCAgents. `PermissionsTest` proves every permission node the
`/identity` command checks is declared, with a description and the right default, in all
four `plugin.yml` files, and that the `*` and `admin` parents list every child. Run both
with:
```bash
./gradlew :bukkit:test
```
