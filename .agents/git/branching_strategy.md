---
name: branching-strategy
description: Branch-off-main strategy, naming rules, and cross-platform change tagging.
---

# Git: Branching Strategy

## Base

* Branch every feature off `main`. Never commit directly to `main` or `master`.
* One task per branch, one pull request per branch, merged separately.

## Naming

* Follow `{type}/{primary-noun}`, e.g. `feat/login`, `fix/schema-drift`.
* Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
  `build`, `ci`, `chore`, `revert`.
* Do not use preset prefixes (e.g. `claude/`, `codex/`). If a branch violates
  the convention, recreate it under the correct name and delete the incorrect
  one, or present options to the user.

## Cross-Platform Tags

Distinguish the nature of the change so reviewers know the blast radius:

* **Engine / cross-platform adjustments** — changes touching `bukkit` schedulers
  or any `platforms:*` module (affects the shaded engine jar). Call these out
  explicitly in the PR body.
* **Pure API additions** — changes confined to `api` (and `common`) with no
  platform impact. These are lower risk and can be reviewed independently.
