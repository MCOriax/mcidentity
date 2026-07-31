# MCOriax Identity Plugin

## Project Overview

* **Platform:** [github.com](https://github.com)
* **Organization:** [MCOriax](https://github.com/MCOriax)
* **Repository:** [mcidentity](https://github.com/MCOriax/mcidentity)
* **Documentation:** [mcoriax.github.io](https://mcoriax.github.io/)

A robust and high-performance identity management system for Minecraft servers. The plugin now **fully supports SpigotMC, PaperMC, and Folia**, including native compatibility with Folia's regionalized multi-threading architecture.

## 🌟 Overview

The MCOriax Identity Plugin provides a complex "Identity" system where players can equip and manage different personas (Identities). Each identity is characterized by its **Profession**, and **Stats**.

Skills, runes, and cosmetics are **not** part of this plugin. They live in the
standalone [MCIdentitySkill](https://github.com/MCOriax/mcidentity-skill),
[MCIdentityRune](https://github.com/MCOriax/mcidentity-rune), and
[MCIdentityCosmetic](https://github.com/MCOriax/mcidentity-cosmetic) add-ons.
Each add-on owns its own storage, keyed by identity UUID, and decides its own
profession restrictions. This plugin focuses purely on identity, stats, and
session management.

Every player has exactly **one primary identity** — their permanent base `player` profession, keyed by their own UUID. Beyond it, players can unlock **multiple alternative base identities**, each of **any other profession** (assigned randomly or chosen by an admin); the `player` profession itself is reserved for the primary identity and can never be an alternative. These bound alternatives are browsable in the paginated Player Identity GUI, stay permanently attached to the player (they cannot be traded, dropped, or deleted), and are granted by admins with `/identity base add <player> [profession] <amount>`. When the central server backend is used, a **cold-boot login protection** check pings the central server during pre-login and politely turns players away while a cold-started server is still waking up.

This system is built with performance and security as top priorities. Identity data can live in **SQLite** (the default, zero-configuration), **MySQL**, **PostgreSQL**, or **MongoDB**, or be owned by the MCIdentity central server and accessed through asynchronous REST calls. Every backend is combined with RAM caching and fully asynchronous access, ensuring a lag-free experience on high-population servers and consistent data across a whole network. It runs across the entire Bukkit family of servers — **SpigotMC**, **PaperMC**, and **Folia**.

## 📚 Documentation

Detailed documentation lives on the project website: **<https://mcoriax.github.io/>**

- [Key Features](https://mcoriax.github.io/features/) — Overview of what the plugin offers.
- [Installation & Setup](https://mcoriax.github.io/installation/) — Getting the plugin running and connecting it to the central server.
- [Commands & Permissions](https://mcoriax.github.io/commands/) — Available commands and required permissions.
- [GUI System](https://mcoriax.github.io/gui/) — The multi-tier interactive menu system.
- [Professions & Custom EXP Gain](https://mcoriax.github.io/profession/) — Defining professions and action-based EXP rules.
- [Data Storage](https://mcoriax.github.io/storage/) — Choosing between SQLite (default), MySQL, PostgreSQL, MongoDB, and the central server.
- [Developer API Usage](https://mcoriax.github.io/api/) — Integrating with `MCIdentityProvider`.
- [Events & Listeners](https://mcoriax.github.io/listeners/) — Custom events for third-party plugins.
- [Skill Add-on](https://mcoriax.github.io/skill/) — Profession restricted skill items, shipped separately as MCIdentitySkill.
- [Rune Add-on](https://mcoriax.github.io/rune/) — Equippable runes with their own GUI and `/rune` command, shipped separately as MCIdentityRune.
- [Cosmetic Add-on](https://mcoriax.github.io/cosmetic/) — Per-identity skins and auras with their own GUI and `/cosmetic` command, shipped separately as MCIdentityCosmetic.
- [Testing](https://mcoriax.github.io/installation/#testing) — Running the unit test suite.

## 💾 Data Storage

The plugin supports multiple storage backends, selected with `database.type`
in `config.yml`:

- **SQLite** (default) — a zero-configuration local file; works out of the box.
- **MySQL** / MariaDB — for networks with an existing MySQL-compatible server.
- **PostgreSQL** — for networks that prefer PostgreSQL.
- **MongoDB** — document-oriented storage.
- **Central server** — the MCIdentity central REST server, sharing one
  authenticated API across a whole network.

See [Data Storage](https://mcoriax.github.io/storage/) for configuration details.

## 📋 Requirements

- **Java 25** or newer (the Gradle build provisions its own JDK 25 toolchain).
- A **SpigotMC**, **PaperMC**, or **Folia** server (build `26.1.x` API line).
- This repository is a plugin extension for those servers — it is not a
  standalone application.
- No external database is required by default (SQLite). MySQL, PostgreSQL,
  MongoDB, or a reachable MCIdentity central server deployment are needed
  only when configured as the backend.
