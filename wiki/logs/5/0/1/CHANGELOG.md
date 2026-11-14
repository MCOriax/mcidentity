# Changelog — 5.0.1

**Released:** 2026-11-14

A patch release. Every platform `plugin.yml` lists MCMultiverse and MCRealm under
`softdepend`, so when either is installed it enables before MCIdentity. Neither is
required, and MCIdentity does not call either of them. It also ships the SonarCloud fixes
merged since 5.0.0: every SQL statement is now a compile-time constant.

## Changed

- Every platform `plugin.yml` (engine, spigotmc, papermc, foliamc) lists `MCMultiverse`
  and `MCRealm` under `softdepend`, keeping `MCAgents` (#153).

## Fixed

- `AbstractSqlDB#getStats` reads a player's stats with one constant statement instead of
  concatenating the column list at runtime, and each dialect returns its insert-if-absent
  statements as finished literals, clearing the SonarCloud SQL hotspot (#150).
- The SQLite, MySQL and PostgreSQL insert-if-absent statements share their column lists in
  `InsertIgnoreStatements`, clearing the duplicated lines SonarCloud found (#151).

## Upgrading

Drop in the new jar. No config, storage, command or permission change.
