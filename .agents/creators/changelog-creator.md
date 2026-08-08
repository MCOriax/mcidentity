---
name: changelog-creator
description: Creates and maintains versioned change logs under wiki/logs/; a new version directory requires user approval.
---

# Creator: Changelog Creator

Creates and maintains files under `wiki/logs/`.

## Path shape

```
wiki/logs/{Major}/{Minor}/{Patch}/{file-name}.md
```

Examples:

* `wiki/logs/1/0/0/CHANGELOG.md`
* `wiki/logs/1/0/1/CHANGELOG.md`
* `wiki/logs/1/1/0/CHANGELOG.md`
* `wiki/logs/2/0/0/CHANGELOG.md`

Rules:

* **Numeric directory segments only** — no `v` prefix, no zero padding.
* The directory shape exists so a version can hold more than one document.
  `CHANGELOG.md` is the default; `MIGRATION.md`, `BREAKING.md`, `UPGRADE.md`, and
  `NOTES.md` may live beside it in the same version directory.

## CHANGELOG.md sections

Use these headings, and **omit the empty ones**:

`Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`

Include the release date and a one-line summary at the top of the file.

## Version approval

**Creating a new version directory requires user approval.** A version directory
*is* a version claim — see [`../rules/versioning.md`](../rules/versioning.md).
Ask which version applies before creating it, and wait for an explicit answer.

The current version of this repository is `project-version` in
`gradle.properties`. Read it; never guess it.

## Never

* Never edit a released version's log to change history. Corrections go in the
  next version's log.
* Never create a version directory speculatively.

## Index maintenance

Keep [`../../wiki/logs/INDEX.md`](../../wiki/logs/INDEX.md) listing every version
**newest first**, one row per version directory, with a one-line summary and the
files it contains. Update it in the same commit that adds or changes a log.

## Branch & Commit Convention

This convention applies to every commit this creator makes. Canonical sources:
[`../git/branching-strategy.md`](../git/branching-strategy.md) and
[`../git/commit-conventions.md`](../git/commit-conventions.md).

### Branching

* Branch off the default branch for every task. Never commit directly to it.
* One task per branch, one pull request per branch.
* Name branches `{type}/{primary-noun}` — e.g. `feat/login`, `fix/schema-drift`,
  `docs/agents-setup`.
* Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
  `build`, `ci`, `chore`, `revert`.
* Never use a tool-preset prefix such as `claude/`, `codex/`, or `cursor/`. If a
  branch violates the convention, recreate it correctly and delete the wrong one,
  or present the options to the user.
* Keep branches short-lived and rebased on the default branch. For multi-task
  work, branches stack in dependency order — see
  [`../planning/task-workflow.md`](../planning/task-workflow.md).

### Commits

* Conventional Commits: `type(optional scope): description`.
* Same type list as above. Scope is a module or subsystem, e.g. `docs(logs):`,
  `feat(identity):`, `chore(deps):`.
* Subject in imperative mood, plain text, no trailing period, no links, no
  issue-tracker IDs.
* Optional body: short bullets explaining what and why.
* Commit each logical change or group of related changes — never batch a whole
  session into one commit. Review the diff before every commit.

Example:

```
docs(logs): add the 4.6.0 changelog

- Record the identity alias round trip under Added
- List the version in wiki/logs/INDEX.md
```

**Pull request titles use a different format** — never a Conventional Commit
prefix. See [`../git/pull-request-template.md`](../git/pull-request-template.md).

## Discovery Protocol

While working, if you notice an instruction worth adding — a new rule, or new
content for an existing instruction file — do NOT create or edit it yourself.
Collect the findings, and when the task is done present them to the user:

* one finding per message block, each in its own code block;
* include the proposed file path, `name`, `description`, and the full proposed
  body;
* explain in one line why it is worth adding.

Then let the user select which findings to apply. Create only the selected ones.
Never batch-apply, never apply silently.

## Reminders

* Placement is governed by
  [`../rules/directories.md`](../rules/directories.md), including creating a new
  folder when none fits.
* Every file created or removed is registered in the index that owns that scope,
  in the same commit, per [`index-creator.md`](index-creator.md).
* Any pull request this creator opens follows
  [`../git/pull-request-template.md`](../git/pull-request-template.md), and
  merging requires user approval per
  [`../planning/task-workflow.md`](../planning/task-workflow.md).
* Version changes require user approval —
  [`../rules/versioning.md`](../rules/versioning.md).
