# Database: Schema Migrations

## Auto-Generation

* Tables (and MongoDB collections/indexes) are auto-generated on first start via
  `initializeSchema`. There is no separate migration runner for the base schema.
* `initializeSchema` runs on every boot and must be idempotent.

## Idempotent Re-Runs

* Guard `CREATE TABLE` / `CREATE TYPE` / `CREATE INDEX` with existence checks
  where the dialect supports them (`IF NOT EXISTS`).
* PostgreSQL: `CREATE TYPE` has no `IF NOT EXISTS`. Gracefully catch and ignore
  the duplicate-type error (`duplicate_object` / SQLState `42710`) on re-run.
* Never drop or recreate existing tables during initialization — that risks
  player data loss.

## Evolving the Schema

* Additive changes (new nullable column, new collection) are preferred.
* Any schema change must stay synchronized across SQLite, MySQL, PostgreSQL, and
  MongoDB — validate with the `schema_validator` tool and update `wiki/database.md`.
