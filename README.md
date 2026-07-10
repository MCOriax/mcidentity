# MCOriax Identity Plugin

A robust and high-performance identity management system for Minecraft servers. The plugin now **fully supports SpigotMC, PaperMC, and Folia**, including native compatibility with Folia's regionalized multi-threading architecture.

## 🌟 Overview

The MCOriax Identity Plugin provides a complex "Identity" system where players can equip and manage different personas (Identities). Each identity is characterized by its **Profession**, and **Stats**.

This system is built with performance and security as top priorities. Identity data can live in **SQLite** (the default, zero-configuration), **MySQL**, **PostgreSQL**, or **MongoDB**, or be owned by the MCIdentity central server and accessed through asynchronous REST calls. Every backend is combined with RAM caching and fully asynchronous access, ensuring a lag-free experience on high-population servers and consistent data across a whole network. It runs across the entire Bukkit family of servers — **SpigotMC**, **PaperMC**, and **Folia**.

## 📚 Documentation

Detailed documentation lives in the [`wiki/`](wiki/) folder:

- [Key Features](wiki/features.md) — Overview of what the plugin offers.
- [Installation & Setup](wiki/installation.md) — Getting the plugin running and connecting it to the central server.
- [Commands & Permissions](wiki/commands.md) — Available commands and required permissions.
- [GUI System](wiki/gui.md) — The multi-tier interactive menu system.
- [Custom Profession EXP Gain](wiki/custom-exp.md) — Defining professions and action-based EXP rules.
- [Data Storage](wiki/database.md) — Choosing between SQLite (default), MySQL, PostgreSQL, MongoDB, and the central server.
- [Developer API Usage](wiki/api.md) — Integrating with `MCIdentityProvider`.
- [Bukkit Events & Integration](wiki/bukkit.md) — Custom events for third-party plugins.
- [Testing](wiki/testing.md) — Running the unit test suite.

## 💾 Data Storage

The plugin supports multiple storage backends, selected with `database.type`
in `config.yml`:

- **SQLite** (default) — a zero-configuration local file; works out of the box.
- **MySQL** / MariaDB — for networks with an existing MySQL-compatible server.
- **PostgreSQL** — for networks that prefer PostgreSQL.
- **MongoDB** — document-oriented storage.
- **Central server** — the MCIdentity central REST server, sharing one
  authenticated API across a whole network.

See [Data Storage](wiki/database.md) for configuration details.

## 📋 Requirements

- **Java 25** or newer (the Gradle build provisions its own JDK 25 toolchain).
- A **SpigotMC**, **PaperMC**, or **Folia** server (build `26.1.x` API line).
- This repository is a plugin extension for those servers — it is not a
  standalone application.
- No external database is required by default (SQLite). MySQL, PostgreSQL,
  MongoDB, or a reachable MCIdentity central server deployment are needed
  only when configured as the backend.
