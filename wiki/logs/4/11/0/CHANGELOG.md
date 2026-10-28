# Changelog — 4.11.0

**Released:** 2026-10-28

*Every `/identity` sub-command checks its own permission node, and the largest classes are
split into single-responsibility helpers with behaviour and public API unchanged.*

## Changed

- **Permissions** (#138) — `/identity`, `help`, `wiki`, `check`, `unequip`, `stats` and `alias` now
  check their own `mcoriax.mcidentity.command.*` node (default `true`); `get` and `base`
  check theirs (default `op`). Before, only `get` and `base` were checked, against
  `mcoriax.mcidentity.admin`.
- `mcoriax.mcidentity.*` now lists `command.alias`, `command.get` and `command.base`.
- The pre-prefix `mcidentity.admin` node is declared again as an `op` parent of
  `mcoriax.mcidentity.admin` and accepted for `get` and `base`, so an old grant keeps working.
- Tab completion and `/identity help` show only the sub-commands the sender may run.
- **Internals** (#139) — `AbstractSqlDB`, `MongoDB` and `AbstractMCIdentityPlugin` delegate to
  package-private helpers; the key, session, stats and profession views every backend built
  on its own are now `common.model` records. No config key, schema, message or public
  method changed.

## Added

- `Permissions.grants`, `Permissions.grantsAnyAdministrative`, `Permissions.SUB_COMMANDS` and
  `Permissions.LEGACY_ADMIN` in `bukkit`.
- `PermissionsTest`, checking the four `plugin.yml` files against `Permissions`.
