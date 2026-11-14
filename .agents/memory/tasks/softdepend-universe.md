---
name: softdepend-universe
description: Task record for listing MCMultiverse and MCRealm under softdepend in every MCIdentity plugin.yml, so they enable first when installed; closes with release 5.0.1.
---

# Soft depend on MCMultiverse and MCRealm

**Status:** in progress

*Opened on project version 5.0.0; closes on 5.0.1.*

## Goal

MCMultiverse and MCRealm create the shared `accounts` table that every MCPlugins plugin
keyed by `account_type` + `account_uuid` points its foreign keys at. Every plugin in the
family lists both under `softdepend`, so when either is installed it enables first and
the load order is the same across the family. Neither is required.

## Objective

* Every platform `plugin.yml` (engine, spigotmc, papermc, foliamc) lists `MCMultiverse`
  and `MCRealm` under `softdepend`, keeping the existing entries. `MCAgents` stays.
* No code calls either plugin, so the build gains no dependency on their jars.
* MCIdentity stores nothing keyed by `account_type` + `account_uuid`, so it does not create
  the shared `accounts` table.
* The SonarCloud fixes merged since 5.0.0 (#150, #151) ship in this release and are in
  its changelog.
* `./gradlew build` passes; Publish and Release are green on 5.0.1.

## Decisions taken while the owner was away

| Question | Choice |
|---|---|
| Adopt the shared `accounts` table | No. Checked every table and collection: none is keyed by `account_type` + `account_uuid`. |
| Version | `5.0.1`: a load-order hint only; nothing changes for an installed server. |

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Record the plan | `plan/softdepend-universe` | |
| 2 | Soft depend on MCMultiverse and MCRealm | `chore/softdepend-universe` | |
| 3 | Release 5.0.1 | `release/5.0.1` | |

## Progress

### Task 1 — plan/softdepend-universe

Record opened. Nothing built yet.
