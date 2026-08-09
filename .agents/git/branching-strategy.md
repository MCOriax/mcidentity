---
name: branching-strategy
description: Branch-off-main strategy, naming rules, and cross-platform change tagging.
---

# Git: Branching Strategy

## Base

* Branch off the default branch for every task. Never commit directly to it.
* One task per branch, one pull request per branch, merged separately.
* Keep branches short-lived and rebased on the default branch.
* For multi-task work, branches **stack in dependency order** — task 1 branches
  off the default branch, task `k` branches off task `k-1`. See
  [`../planning/task-workflow.md`](../planning/task-workflow.md).

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
