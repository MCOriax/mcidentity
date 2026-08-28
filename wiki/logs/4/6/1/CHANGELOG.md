# Changelog — 4.6.1

**Released:** 2026-08-27

Clears the SonarCloud backlog: three security and reliability defects, two
assertion-free tests, 27 duplicated string literals, and 21 methods over the
cognitive-complexity limit. Also adopts the shared agent instruction set over the
`lxagents-agents-base` connector and removes the local copies that shadowed it.
No plugin behavior changed in this release.

## Added

- `.agents/prompts/sonarqube-remediation.md` — verify every SonarCloud finding
  against the working tree before tracking it, group remediation by kind, and
  order extractions ahead of restructurings.
- `StatKeys.all()` — exposes the stat allow-list so backends can render statement
  text from its own literals.
- `SQLiteDBRoundTripTest.loggedActions` — reads the audit table back over JDBC, so
  `logActions` can assert the rows it writes.
- `.agents/memory/` with the record of the duplicate-instruction audit.
- Named constants for 27 previously duplicated string literals across
  `MCIdentityCentralServer`, `MongoDB`, `DatabaseFactory`, `AbstractSqlDB`,
  `AbstractMCIdentityPlugin`, `MCIdentityTabCompleter`, `HandleBaseCommand`, and
  `MCIdentityStatsListener`.

## Changed

- 21 methods refactored below a cognitive complexity of 15, from
  `InitializeIdentity.execute` at 122 down to three methods at 16. Behavior,
  scheduler usage, and Folia region threading are unchanged throughout.
- `AbstractSqlDB` pre-renders the stat select and increment statements once per
  allow-listed key, so a caller's stat key is only ever a map lookup.
- `AGENTS.md` now resolves the shared instruction set through the connector and
  mirrors its auto-activation trigger table, with this repository's own rows
  appended.
- `.agents/rules/repository.md` gained the three repository-specific rules the
  deleted duplicates carried: the engine versus pure-API blast radius, what
  verifying a task means here, and the version-carrier table.
- `schemaIsIdempotent` now asserts that re-initialization preserves existing rows
  and stats, rather than only that it does not throw.

## Removed

- Eleven `.agents/` files that shadowed a shared instruction by `name`: the four
  creators, `git/branching-strategy.md`, `git/commit-conventions.md`,
  `git/pull-request-template.md`, `planning/task-workflow.md`,
  `prompts/branch-and-commit.md`, `rules/directories.md`, and
  `rules/versioning.md`. All eleven are served by the `lxagents-agents-base`
  connector and are read from `agents://` instead.

## Fixed

- `PassiveExpService.stop` re-interrupts the thread when the shutdown flush is
  interrupted, instead of swallowing the `InterruptedException`.
- Every `printStackTrace` call in `bukkit` replaced with a plugin-logger call that
  carries the throwable, so stack traces reach the server log with their message
  instead of bypassing the log configuration on stderr.

## Security

- `AbstractSqlDB.getStat` and `addStat` no longer concatenate a caller-supplied
  stat key into SQL. Both statements are rendered at class-initialization time
  from `StatKeys.all()`, so the only strings reaching statement text are in-code
  literals, and an unknown key produces no statement at all.
