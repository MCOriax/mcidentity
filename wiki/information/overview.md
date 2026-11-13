# Project Overview

[← Back to README](../../README.md)

## Project identity

| | |
|---|---|
| **Platform** | [github.com](https://github.com) |
| **Organization** | [MCOriax](https://github.com/MCOriax) |
| **Repository** | [mcidentity](https://github.com/MCOriax/mcidentity) |
| **Website** | [mcoriax.github.io](https://mcoriax.github.io/) |

A robust, high-performance profession manager for Minecraft servers. The
plugin fully supports **SpigotMC**, **PaperMC**, and **Folia**, including native
compatibility with Folia's regionalized multi-threading architecture.

## What the plugin does

The MCOriax Identity Plugin is a **profession manager**. Every player has one
character, keyed by their own player UUID, with nine **Stats** and a pool of stat
points. On top of it they **wear one profession at a time**, such as miner or
fighter. A player who wears nothing wears the built-in base `player` profession.

* Each profession keeps its own **level and EXP** per player, so switching back to
  a profession resumes where the player left it.
* Stats and stat points belong to the player and follow them whatever they wear.
* Professions are worn with `/identity wear <profession>`, taken off with
  `/identity unwear`, or picked in the profession GUI. Which professions a player
  may wear is decided by permission nodes.
* The vanilla inventory and enderchest are never touched.

Every other plugin binds personal data to `account_type = "player"` and
`account_uuid = <player UUID>`, so a profession change never moves a wallet,
membership or anything else.

When the central server backend is used, a **cold-boot login protection** check
pings the central server during pre-login and politely turns players away while a
cold-started server is still waking up.

## Scope boundary — what lives elsewhere

Skills, runes, and cosmetics are **not** part of this plugin. They are standalone
add-ons:

| Add-on | Repository | Owns |
|---|---|---|
| MCIdentitySkill | [mcidentity-skill](https://github.com/MCOriax/mcidentity-skill) | Profession-restricted skill items, stored on the `ItemStack` PDC. |
| MCIdentityRune | [mcidentity-rune](https://github.com/MCOriax/mcidentity-rune) | Equippable runes with their own GUI and `/rune` command. |
| MCIdentityCosmetic | [mcidentity-cosmetic](https://github.com/MCOriax/mcidentity-cosmetic) | Per-player skins and auras with their own GUI and `/cosmetic` command. |

Each add-on creates and owns its own tables, keyed by the player UUID, with no
foreign key onto this plugin's tables, and decides its own profession
restrictions. This repository focuses purely on **professions, stats, and session
management**. It stores no inventory or enderchest; player storage belongs to the
planned MCMultiverse and MCRealm plugins.

## Performance and storage posture

The system is built with performance and security as top priorities. Identity data
can live in:

* **SQLite** — the default, zero-configuration local file;
* **MySQL** / MariaDB — for networks with an existing MySQL-compatible server;
* **PostgreSQL** — for networks that prefer PostgreSQL;
* **MongoDB** — document-oriented storage;
* **Central server** — the MCIdentity central REST server, sharing one
  authenticated API across a whole network.

Every backend is combined with RAM caching and fully asynchronous access, which
keeps high-population servers lag-free and data consistent across a network.
Backend selection is the `database.type` key in `config.yml` — see
[Data Storage](../reference/database.md).

## Where to go next

| Page | For |
|---|---|
| [Key Features](features.md) | What the plugin offers, feature by feature. |
| [System Architecture](architecture.md) | Gradle modules, dependency rules, storage abstraction. |
| [Requirements](../environments/requirements.md) | What is needed to build and run. |
| [Installation & Setup](../environments/installation.md) | Getting the plugin running. |
