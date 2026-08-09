---
name: identity-data-model
description: Primary vs. alternative identities, deterministic UUID derivation, and the RAM caching layer.
---

# Knowledge: Identity Data Model

MCIdentity models a player as one primary identity plus optional alternative
identities.

## Base Identities

* **Primary Base Identity** — backed by the permanent built-in `player`
  profession reserved for the primary identity, keyed by the player's own UUID.
  Defined in `BaseProfession`. The `player` profession is hardcoded in memory,
  cannot be configured via YAML, and can never be used as an alternative
  sub-identity.
* **Alternative Base Identities** — extra slots (1…n) a player can unlock. Each
  alternative is assigned a random or chosen non-player profession when granted
  via `MCIdentityProvider.addBaseProfession`. Unlocked slot counts are cached in
  `MCIdentityManager`.

## Deterministic UUID Derivation

Alternative base identities have deterministic UUIDs derived from the player's
UUID and the unlocked slot index (1…n) via `BaseProfession.identityUuid`. The
same inputs always yield the same UUID, so identities are stable across sessions
and servers without extra storage.

## Caching Layer

* `ActiveIdentitySession` (inside `MCIdentityManager`) holds the currently
  active identity's live state in RAM.
* Updates apply to the in-memory session first for instant feedback, then flush
  to the database asynchronously. See `performance/ram-caching-rules.md`.
* Reads prefer the cache; on a cold cache the backend is queried async and
  failures degrade safely (`ethics/safe-degradation.md`).
