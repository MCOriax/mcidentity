# MCOriax Identity Plugin

A robust and high-performance identity management system for Minecraft servers. The plugin now **fully supports SpigotMC, PaperMC, and Folia**, including native compatibility with Folia's regionalized multi-threading architecture.

## 🌟 Overview

The MCOriax Identity Plugin provides a complex "Identity" system where players can equip and manage different personas (Identities). Each identity is characterized by its **Profession**, and **Stats**.

This system is built with performance and security as top priorities. All identity data is owned by the MCIdentity central server and accessed through asynchronous REST calls combined with RAM caching, ensuring a lag-free experience on high-population servers and consistent data across a whole network. It runs across the entire Bukkit family of servers — **SpigotMC**, **PaperMC**, and **Folia**.

## 📚 Documentation

Detailed documentation lives in the [`wiki/`](wiki/) folder:

- [Key Features](wiki/features.md) — Overview of what the plugin offers.
- [Installation & Setup](wiki/installation.md) — Getting the plugin running and connecting it to the central server.
- [Commands & Permissions](wiki/commands.md) — Available commands and required permissions.
- [GUI System](wiki/gui.md) — The multi-tier interactive menu system.
- [Custom Profession EXP Gain](wiki/custom-exp.md) — Defining professions and action-based EXP rules.
- [Data Storage](wiki/database.md) — How identity data lives on the central server.
- [Developer API Usage](wiki/api.md) — Integrating with `MCIdentityProvider`.
- [Bukkit Events & Integration](wiki/bukkit.md) — Custom events for third-party plugins.
- [Testing](wiki/testing.md) — Running the unit test suite.
