# Project Overview

[← Back to README](../../README.md)

## Project identity

| | |
|---|---|
| **Platform** | [github.com](https://github.com) |
| **Organization** | [MCOriax](https://github.com/MCOriax) |
| **Repository** | [mcidentity](https://github.com/MCOriax/mcidentity) |
| **Website** | [mcoriax.github.io](https://mcoriax.github.io/) |

A robust, high-performance identity management system for Minecraft servers. The
plugin fully supports **SpigotMC**, **PaperMC**, and **Folia**, including native
compatibility with Folia's regionalized multi-threading architecture.

## What the plugin does

The MCOriax Identity Plugin provides an "Identity" system where players equip and
manage different personas. Each identity is characterized by its **Profession**
and its **Stats**.

Every player has exactly **one primary identity** — their permanent base `player`
profession, keyed by their own UUID. Beyond it, players can unlock **multiple
alternative base identities**, each of **any other profession**, assigned randomly
or chosen by an admin. The `player` profession itself is reserved for the primary
identity and can never be an alternative.

These bound alternatives are:

* browsable in the paginated Player Identity GUI;
* permanently attached to the player — they cannot be traded, dropped, or
  deleted;
* granted by admins with `/identity base add <player> [profession] <amount>`.

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
| MCIdentityCosmetic | [mcidentity-cosmetic](https://github.com/MCOriax/mcidentity-cosmetic) | Per-identity skins and auras with their own GUI and `/cosmetic` command. |

Each add-on creates and owns its own tables, keyed by identity UUID, with no
foreign key onto this plugin's tables, and decides its own profession
restrictions. This repository focuses purely on **identity, stats, and session
management**.

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
