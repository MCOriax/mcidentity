---
name: commit-conventions
description: Conventional Commits formatting rules for commit messages.
---

# Git: Commit Conventions

Use Conventional Commits: `type(optional scope): description`.

## Types

`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`,
`chore`, `revert`.

## Scopes

Prefer a module or subsystem scope, e.g. `feat(database):`, `fix(folia):`,
`feat(identity):`, `perf(cache):`, `docs(wiki):`.

## Rules

* Subject line in plain text, imperative mood, no trailing period.
* No links and no issue-tracker IDs in the message.
* Body (optional) explains what and why as short bullet points.
* Commit each change or group of related changes; do not batch an entire
  session into one commit. Always review the diff before committing.

## Examples

```
feat(database): add PostgreSQL backend

- Implement IMCIdentityDB via AbstractSqlDB
- Register type in MCIdentityManager.createDatabase
```

```
fix(folia): hop inventory writes back onto EntityScheduler
```
