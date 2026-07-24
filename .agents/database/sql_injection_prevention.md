# Database: SQL Injection Prevention

## Mandates

* Use `PreparedStatement` for ALL JDBC calls. Never concatenate user or player
  input into SQL strings.
* Bind every value as a parameter. Do not interpolate values into the SQL text.

## Dynamic Identifiers

* Column and table names cannot be bound as parameters. Where a column name is
  dynamic (notably stats), it MUST be validated against the `StatKeys`
  allow-list before it is placed into the query.
* Reject anything not in the allow-list — do not attempt to sanitize by escaping.

## Guidance

* Centralize query construction in `AbstractSqlDB` so the allow-list check
  cannot be bypassed by a single backend.
* MongoDB: use typed document builders / field constants, never string-built
  queries; validate dynamic field names against the same allow-list.
