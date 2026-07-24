---
name: ram-caching-rules
description: RAM-first update order, async writes, and passive EXP flush rules for in-memory state.
---

# Performance: RAM Caching Rules

Rules for manipulating `MCIdentityManager`'s in-memory state.

## Update Order

1. **RAM first.** Update properties in memory (the `ActiveIdentitySession`) for
   instant player feedback.
2. **Async write.** Persist the change to the database asynchronously; do not
   block the game thread.
3. **Reconcile on failure.** If the write fails, keep the last known-good RAM
   state and degrade safely — never overwrite RAM with a partial result.

## Passive EXP

* Passive base-profession EXP accrues in memory and is flushed in bulk by the
  buffered passive EXP service (`bukkit/util`), not written per-tick.
* Flush on logout, on identity switch, and on a periodic interval.

## Cache Discipline

* Reads prefer the cache; cold reads query the backend async and populate the
  cache.
* Keep cached slot counts (unlocked base-identity slots) consistent with the
  backend after any grant.
