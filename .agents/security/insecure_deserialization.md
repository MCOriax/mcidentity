---
name: security-insecure-deserialization
description: Safe handling of serialized ItemStack/inventory blobs to prevent deserialization attacks.
---

# Security: Insecure Deserialization

MCIdentity serializes inventories and items to Base64 with
`BukkitObjectOutputStream` and reads them back via
`BukkitObjectInputStream.readObject()` (`bukkit/util/ItemStackSerializer`).
`readObject` reconstructs arbitrary serializable objects, so treating a stored
blob as trusted is a remote-code-execution surface if the storage layer is ever
tampered with (a shared MySQL/PostgreSQL instance, a compromised central server,
or hand-edited rows).

## Rules

* Only ever deserialize blobs that MCIdentity itself wrote. Never deserialize
  item/inventory data supplied directly by a player, a command argument, or an
  untrusted third-party plugin.
* Keep deserialization confined to `ItemStackSerializer`; do not scatter
  `BukkitObjectInputStream` / `readObject` calls across listeners and commands.
* Guard every decode: catch `IOException` / `ClassNotFoundException` / decode
  errors and degrade safely (empty inventory, skip the slot) rather than letting
  the exception reach the game thread. See `ethics/safe_degradation.md`.
* Validate the result after decoding (non-null `ItemStack`, expected array
  length) before applying it to a player.
* For any NEW cross-boundary data prefer the typed JSON DTOs already used for the
  central server; do not introduce Java-native `ObjectInputStream` usage for
  network or config payloads.

## Storage integrity

* These blobs live in the same tables as other identity data, so the database
  credentials and central-server token are the trust boundary — keep them secret
  (`security/secret_management.md`) and validate stat/column names against the
  `StatKeys` allow-list (`database/sql_injection_prevention.md`).
