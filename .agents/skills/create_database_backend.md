---
name: create-database-backend
description: Skill for implementing a new storage backend behind the IMCIdentityDB persistence contract.
---

# Skill: Create a Database Backend

Implement a new storage backend behind the `IMCIdentityDB` persistence contract.

## Steps

1. **Implement the contract.** Create the backend under `common/db` (or
   `common/central` for a remote service). Implement every method of
   `api/db/IMCIdentityDB`. For SQL dialects, extend `AbstractSqlDB` to reuse the
   shared JDBC query logic instead of duplicating it.
2. **Register in the factory.** Add the new type to `database.type` handling in
   `MCIdentityManager.createDatabase` so the backend is selectable from config.
3. **Async only.** Every query must run asynchronously on the daemon
   `ExecutorService` and return via `CompletableFuture`. Never block the main
   Bukkit/Folia thread. See `performance/async_first_policy.md`.
4. **Validate inputs.** Dynamic column names (e.g. stats) must be validated
   against the `StatKeys` allow-list. Use `PreparedStatement` for all JDBC
   calls. See `database/sql_injection_prevention.md`.
5. **Schema.** Generate tables on first start via `initializeSchema` and
   gracefully ignore duplicate-type errors on re-run. See
   `database/schema_migrations.md`.
6. **Safe degradation.** Read failures resolve to neutral fallbacks (empty
   optionals, zero values), never a thrown exception on the game thread.
7. **Test.** Add coverage under `common/src/test/java` mirroring the existing
   backends (real SQLite file, mocked drivers for MySQL/PostgreSQL/MongoDB).
8. **Docs.** Update `wiki/database.md` and `wiki/environment.md`, and update
   `INDEX.md` if new files are added.
