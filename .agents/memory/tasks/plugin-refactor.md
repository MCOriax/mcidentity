---
name: memory-task-plugin-refactor
description: Record of the command permission audit and the single-responsibility split of MCIdentity's largest classes, closed by a release.
---

# Task — Plugin Refactor

*Opened on project version 4.10.0.*

**Status:** in progress — 2 of 3

## Goal

MCIdentity is easier to read and change, and no command a normal player should not run is
open to them: every source file has one responsibility, the oversized classes are split,
and every `/identity` sub-command is guarded by a node declared in `plugin.yml` with a
sensible default.

## Objective

* Every `/identity` sub-command checks its own node; administrative ones default to `op`,
  player ones to `true`. Every node checked in code is declared, with a description and a
  default, in all four `plugin.yml` files, and the `*` and `admin` parents list every child.
* Tab completion and `/identity help` hide what the sender lacks.
* `AbstractSqlDB`, `MongoDB`, `AbstractMCIdentityPlugin`, `MCIdentityProvider`,
  `MCIdentityManager` and `MCIdentityCentralServer` are split into single-responsibility
  classes with behaviour, public API, config keys, schemas and messages unchanged.
* `./gradlew build` passes; the chain merges in order and Publish/Release are green.

## Detail and constraints

* The add-ons (`mcidentity-skill`, `-cosmetic`, `-rune`) and MCMembership compile against,
  or reflect into, the published `api`, `common` and `bukkit` artifacts: no public class,
  method or package is renamed or removed. Splits extract package-private helpers and
  delegate.
* `api` and `common` stay free of platform types; Folia threading is unchanged.
* The owner was away and pre-approved the plan, the pull requests, the merge order and
  version `4.11.0`.

## Decisions taken while the owner was away

| Question | Choice |
|---|---|
| Legacy `mcidentity.admin` (checked before #93, still named in the docs) | Declared again as an `op` parent of `mcoriax.mcidentity.admin`, and accepted in code for the administrative nodes, so an old grant keeps working. |
| Should `/identity` carry a command-level `permission:` | No: revoking `command.use` would then lock every sub-command; each sub-command checks its own node instead. |
| Does `/identity get` or `base` also accept `mcoriax.mcidentity.admin` in code | Yes, so a permissions plugin that does not expand `plugin.yml` children still lets admins through. |
| Version | `4.11.0`: new nodes are a new surface, nothing breaks. |

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Command permissions and this record | `refactor/command-permissions` | |
| 2 | Single-responsibility split | `refactor/single-responsibility` | |
| 3 | Release | `release/4.11.0` | |

## Entries

### Task 1 — refactor/command-permissions

* Audit of `/identity` (the plugin's only command; console is refused, the GUIs open no
  admin action): `use`, `help`, `wiki`, `check`, `unequip`, `stats` and `alias` are
  player-facing (`true`); `get` (mints identity items) and `base` (changes another
  player's slots) are administrative (`op`).
* Before: only `get` and `base` were checked, both against `mcoriax.mcidentity.admin`;
  the seven player nodes were declared but never read, and `command.alias` was missing
  from the `mcoriax.mcidentity.*` children.
* `MCIdentityCommand` now checks each sub-command's node before dispatch;
  `Permissions.SUB_COMMANDS` maps sub-command to node, and `Permissions.grants` also
  accepts `admin` and the legacy `mcidentity.admin` for `get` and `base`.
  `MCIdentityTabCompleter` and `SendHelp` filter on the same nodes.
* All four `plugin.yml` files: `*` lists every node (alias, get and base added),
  `mcidentity.admin` is declared as a legacy parent. `PermissionsTest` (bukkit) checks
  the four files against `Permissions`.
* `wiki/reference/commands.md` carries the node table; `wiki/guides/testing.md` and the
  repository map list the new test.
* Next: task 2 splits the hot-spot classes.

### Task 2 — refactor/single-responsibility

* `common.model` gains `IdentityKeyRecord`, `IdentitySessionRecord`, `IdentityStatsRecord`
  and `ConfiguredProfession`, replacing the anonymous views duplicated across
  `AbstractSqlDB`, `MongoDB`, `MCIdentityCentralServer`, `MCIdentityProvider` and
  `BaseIdentityRegistrar`.
* `AbstractSqlDB` (889 → ~600 lines) delegates to package-private `SqlAsyncRunner`,
  `SqlSchema`, `SqlTransactions` (one commit/rollback block instead of three),
  `SqlIdentityRows` and `StatStatements`; every protected hook is kept.
* `MongoDB` (628 → ~430) keeps its operations; the layout and mappings moved to
  `MongoDocuments`, start-up drops and indexes to `MongoSchema`. `DatabaseExecutors`
  and `StorableProfession` serve both backend families.
* `AbstractMCIdentityPlugin` (598 → ~220) keeps its lifecycle and public getters;
  `ProfessionConfigLoader`, `StorageBootstrap` and `ListenerRegistrar` (package-private)
  do the work.
* Left as they are: `MCIdentityProvider` and `MCIdentityManager` are already public
  facades over `SessionCache`, `ProfessionRegistry`, `PlayerStateStore` and
  `BaseIdentityRegistrar`, and add-ons reflect into the provider by method name;
  `MCIdentityCentralServer` already sends through `CentralHttpClient`. Splitting them
  further would move public methods.
* No public type, method, config key, schema or message changed. `wiki/information/architecture.md`
  and the repository map name the new classes.
* Next: the release task.
