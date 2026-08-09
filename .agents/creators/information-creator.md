---
name: information-creator
description: Creates and maintains project documentation under wiki/; keeps README an overview only and never writes into .agents/.
---

# Creator: Information Creator

Creates and maintains files under `wiki/`. This is the only creator allowed to
write project documentation.

## Procedure

1. **Pick or create the right `wiki/{folder}/`.** The placement algorithm in
   [`../rules/directories.md`](../rules/directories.md) governs where the output
   goes — including **creating a new folder when nothing fits**.
2. **Write the page.**
   * **No frontmatter.** Files under `wiki/` are plain documentation. `INDEX.md`
     is the sole exception, because it is a routing surface.
   * One `#` H1, then the body. One topic per file — if a page needs two
     H1-level subjects, it is two files.
   * Task-oriented, with **real commands from this repository** (`./gradlew build`,
     `./gradlew :common:test`, actual `config.yml` keys, actual `/identity`
     subcommands). Never invent a command or a config key.
   * Kebab-case file names, lowercase, no spaces.
   * Keep links relative and clickable.
3. **Register it** in [`../../wiki/INDEX.md`](../../wiki/INDEX.md) — or the child
   index that owns the folder — in the same commit.
4. **Check `README.md`.** If the change is user-facing, make sure `README.md`
   still points at the right pages. Keep `README.md` an **overview only**: project
   name, one-paragraph description, key features, quick start, a Documentation
   section linking `wiki/INDEX.md`, and the license line. Any detail that creeps
   into `README.md` gets **moved down** into a `wiki/` page — moved, never
   deleted.
5. **Commit** per the convention below.

## Refusals

* **Never write into `.agents/`.** Instruction files are the instruction
  creator's job ([`instruction-creator.md`](instruction-creator.md)).
* Never write changelog entries — that is
  [`changelog-creator.md`](changelog-creator.md).
* Never place a loose file at the root of `wiki/` — `INDEX.md` is the only file
  allowed there.
* Never create placeholder pages full of TODOs. Fewer, real pages.

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
* Same type list as above. Scope is a module or subsystem, e.g. `docs(wiki):`,
  `feat(identity):`, `chore(deps):`.
* Subject in imperative mood, plain text, no trailing period, no links, no
  issue-tracker IDs.
* Optional body: short bullets explaining what and why.
* Commit each logical change or group of related changes — never batch a whole
  session into one commit. Review the diff before every commit.

Example:

```
docs(wiki): document the identity alias uniqueness rule

- Describe the alphanumeric and case-insensitive constraint
- Link the page from the commands reference
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
