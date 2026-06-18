# Testing

[← Back to README](../README.md)

The plugin includes a suite of unit tests for the `common` module to ensure data integrity and logic correctness.

## Running Tests
To run the unit tests, use the following Gradle command:
```bash
./gradlew :common:test
```
The tests cover:
- Core Managers (`MCIdentityManager`, `MCIdentityProvider`)
- Database Providers (SQLite, PostgreSQL, MongoDB)
- Data serialization and session management logic.
