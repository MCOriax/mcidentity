# Requirements

[← Back to README](../README.md)

This document lists what the **MCOriax Identity Plugin** needs to build and run
(runtime requirements), what it is responsible for doing (functional
requirements), and the qualities it must uphold (non functional requirements).
See [System Architecture](system.md) for how these are implemented and
[Data Storage](database.md) for backend configuration.

**Contents**

1. [Runtime requirements](#1-runtime-requirements)
2. [Build requirements](#2-build-requirements)
3. [Functional requirements](#3-functional-requirements)
4. [Non functional requirements](#4-non-functional-requirements)
5. [Out of scope](#5-out-of-scope)

---

## 1. Runtime requirements

| Requirement | Detail |
|---|---|
| **Server platform** | A **SpigotMC**, **PaperMC**, or **Folia** server on the `26.1.x` API line. |
| **Java** | **Java 25** or newer on the server. |
| **Database** | None by default — **SQLite** is a zero configuration local file. **MySQL**/MariaDB, **PostgreSQL**, **MongoDB**, or a reachable **MCIdentity central server** ([`server-expressjs`](https://github.com/MCOriax/server-expressjs)) are required only when configured as the backend. |
| **Drivers** | The SQLite and MySQL JDBC drivers ship with the server platforms; the PostgreSQL driver and MongoDB client are bundled in the plugin jar, so no backend needs extra downloads. |

This repository is a plugin extension for those servers — it is not a standalone
application.

---

## 2. Build requirements

| Requirement | Detail |
|---|---|
| **Gradle** | Use the bundled Gradle wrapper (`./gradlew`); it provisions its own **JDK 25** toolchain, so no system JDK 25 is required to build. |
| **Multi project build** | The build is a Gradle multi project defined in `settings.gradle`; modules are `api`, `common`, `bukkit`, and the three `platforms:*` entry points. |
| **Publishing (optional)** | The `api`, `common`, and `bukkit` library modules publish to GitHub Packages, which reads the `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables. See [Environment Variables](environment.md). |

---

## 3. Functional requirements

- **Identity system.** Players equip and manage personas (Identities), each with
  a profession and nine stats. Identities are represented as physical, named
  in game items.
- **Primary and alternative base identities.** Every player permanently owns one
  primary `player` identity keyed by their own UUID, and can unlock multiple
  alternative base identities of any other profession, granted by admins with
  `/identity base add`.
- **Professions and custom EXP.** Professions are configurable, with action
  based EXP rules and passive base leveling.
- **Interactive GUIs.** Multi tier virtual menus for identities, runes, and
  abilities, bound to live data.
- **Commands and permissions.** A role based `/identity` command tree guards
  administrative actions.
- **Selectable storage.** Identity data lives in SQLite (default), MySQL,
  PostgreSQL, MongoDB, or the MCIdentity central server, all behind one
  `IMCIdentityDB` contract.
- **Cold boot login protection.** With the `central` backend, the plugin pings
  the central server's `/health` route during pre login to wake a suspended
  deployment and refuse joins until data is reachable.
- **Third party API.** A central `MCIdentityProvider` plus custom Bukkit events
  let other plugins integrate with the identity system.

The full feature list is in [Key Features](features.md); commands are in
[Commands](commands.md).

---

## 4. Non functional requirements

- **Multi platform.** One codebase runs on SpigotMC, PaperMC, and Folia,
  including native compatibility with Folia's regionized scheduler.
- **Performance.** Every storage operation runs asynchronously off the main game
  thread and is combined with RAM caching, keeping high population servers lag
  free.
- **Resilience.** Reads that fail resolve to safe fallbacks (empty results or
  zero) and are logged, so a storage hiccup never crashes gameplay; central
  server initialization retries automatically.
- **Security.** Administrative commands are permission gated, stat identifiers
  are validated against a fixed allow list, and central server calls carry a
  bearer token over a pooled HTTP client.
- **Portability.** The SQLite schema matches the central server's, so a database
  file can move between a game server and a central deployment.

---

## 5. Out of scope

- The plugin does not host the shared database itself; the central server
  ([`server-expressjs`](https://github.com/MCOriax/server-expressjs)) owns that
  when the `central` backend is used.
- Ability behaviour is provided by **MythicMobs**; the plugin only records which
  ability (`abilityCode`) and model (`abilitySkin`) an identity equips.
- The base `player` profession is hardcoded in memory and is intentionally not
  configurable through YAML.
