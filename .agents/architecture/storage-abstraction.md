---
name: storage-abstraction
description: Rule that IMCIdentityDB is the single persistence facade game code depends on.
---

# Architecture: Storage Abstraction

## Rule

* `IMCIdentityDB` is the single persistence facade. Game code must NEVER be aware
  of the underlying storage type — SQLite, MySQL, PostgreSQL, MongoDB, or the
  MCIdentity central server.
* Backend selection happens exactly once, in `MCIdentityManager.createDatabase`,
  driven by `database.type` config. Everything else depends only on the
  interface.

## Guidance

* Share SQL logic through `AbstractSqlDB`; keep dialect specifics inside the
  concrete backend.
* The `MCIdentityProvider` layers the RAM session cache on top of the facade —
  callers use the provider, not a concrete backend.
* New backends implement the full `IMCIdentityDB` contract and register in the
  factory (`skills/create-database-backend.md`). Do not add backend-specific
  methods to game-facing code.
