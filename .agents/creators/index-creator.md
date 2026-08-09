---
name: index-creator
description: Owns the shape of every INDEX.md in the repository — the canonical template, the split threshold, and the audit procedure.
---

# Creator: Index Creator

Owns the shape of every `INDEX.md` in the repository, so they all look the same.

## What an index may contain

Pointer tables, a scope line, and a parent link. **Nothing else.**

An index never explains a rule, never documents behavior, and never carries prose
beyond a one-line purpose per row. The moment an index starts teaching something,
it has become the context bloat it exists to prevent — move that content into a
real file and link to it.

## Split threshold — when a folder earns its own `INDEX.md`

* A folder gets its own `INDEX.md` when it holds **more than ~10 files**, or when
  it has **subfolders of its own**.
* Below that threshold, the parent index lists the folder's files inline. Do not
  create an index for a folder with three files — index sprawl costs more hops
  than it saves.
* When a folder crosses the threshold, move its rows out of the parent into a new
  child `INDEX.md`, replace them in the parent with a single "Child Indexes" row,
  and add the child to the root `INDEX.md` — all in one commit.
* Every index names its parent. Every child index is reachable from the root.

## Canonical template

Copy this shape for every non-root index:

```
---
name: {scope}-index
description: Index of {scope} — {what an agent finds here}.
---

# {Scope} Index

**Scope:** `{directory this index owns}`
**Parent:** [{parent index name}]({relative path to parent INDEX.md})

## {Section — one per subfolder or topic}

| File | Purpose |
|---|---|
| [`{path}`]({path}) | {One line. What it is for, not what it says.} |

## Child Indexes

| Index | Scope | Load when |
|---|---|---|
| [`{sub}/INDEX.md`]({sub}/INDEX.md) | {what lives under `{sub}/`} | {the condition that makes this branch the right one} |
```

Drop the `Child Indexes` section when the scope has no child index.

## Root-index variant

The root `INDEX.md` is different: **no `Scope` line, no `Parent` line, no file
table**. Its entire body is a single Child Indexes table covering every index in
the repository, plus at most a two-line note.

```
---
name: root-index
description: Root router — points to every other index in the repository.
---

# Repository Index

| Index | Scope | Load when |
|---|---|---|
| [`{path}/INDEX.md`]({path}/INDEX.md) | {scope} | {the condition that makes this branch the right one} |
```

The root index lists indexes only — never rules, never documentation, never links
to leaf content.

## Maintenance rules

* A file **added, removed, moved, or renamed** updates its owning index **in the
  same commit**.
* An index **added, removed, or renamed** updates the root `INDEX.md` **in the
  same commit**.
* A `Purpose` cell is one line and never grows into a paragraph.
* Rows are sorted so the most-used entries come first.
* `INDEX.md` is the one uppercase file name; everything else under `.agents/` and
  `wiki/` is kebab-case.

## No orphans

* Every `INDEX.md` must be reachable from the root `INDEX.md`.
* Every file in an indexed scope must appear in **exactly one** index.

## Audit procedure

Run this whenever the tree changes shape, and report both lists:

1. Walk `.agents/` and `wiki/`, collecting every file.
2. For each file, find the index row that points at it. List every file that has
   **no** row — these are unindexed.
3. For each index row, resolve its link target. List every row whose target does
   **not** exist — these are dangling.
4. Confirm every `INDEX.md` appears in the root router, and that each names its
   parent.
5. Confirm no folder above the split threshold lacks an index, and no folder
   below it has one.

Report both lists to the user. Do not silently repair rows that imply a missing
file — a dangling row may mean the file was deleted by mistake.

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
* Same type list as above. Scope is a module or subsystem, e.g. `docs(index):`,
  `feat(identity):`, `chore(deps):`.
* Subject in imperative mood, plain text, no trailing period, no links, no
  issue-tracker IDs.
* Optional body: short bullets explaining what and why.
* Commit each logical change or group of related changes — never batch a whole
  session into one commit. Review the diff before every commit.

Example:

```
docs(index): split the security folder into its own index

- Move eleven security rows out of .agents/INDEX.md
- Register the child index in the root router
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
  in the same commit.
* Any pull request this creator opens follows
  [`../git/pull-request-template.md`](../git/pull-request-template.md), and
  merging requires user approval per
  [`../planning/task-workflow.md`](../planning/task-workflow.md).
* Version changes require user approval —
  [`../rules/versioning.md`](../rules/versioning.md).
