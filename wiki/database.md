# Database Schema

[← Back to README](../README.md)

The plugin maintains several tables to store identity data. Data access is backed by **HikariCP** connection pooling, and the plugin supports **SQLite**, **PostgreSQL**, and **MongoDB** backends.

## Main Tables
- **`identity_keys`**: Primary registry of identities and their professions.
- **`identity_properties`**: Stores level, experience, and base head texture.
- **`identity_stats`**: Stores all numerical attributes (energy, health, strength, etc.).
- **`identity_cosmetics`**: Stores active **skin** (Base64) and **aura** (system code) for each identity.
- **`identity_equipped_abilities`**: Maps equipped abilities to specific slots.
- **`identity_equipped_runes`**: Maps equipped runes to specific slots.
- **`identity_sessions`**: Tracks which identity is currently equipped by which player.

## Archive Tables
Archived versions of all main tables (prefixed with `archived_`) are used to store data when an identity is deleted or modified significantly, ensuring data recovery is possible.
