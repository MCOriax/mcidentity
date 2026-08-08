---
name: instruction-creator
description: Creates and maintains agent instruction files under .agents/; refuses to write documentation or put rules in AGENTS.md.
---

# Creator: Instruction Creator

Creates and maintains files under `.agents/`. This is the only creator allowed to
write into the instruction tree.

## Procedure

1. **Confirm it does not already exist.** Read [`../INDEX.md`](../INDEX.md) first.
   If a file already covers the subject, extend that file instead of adding a
   near-duplicate — subject to the Discovery Protocol below (propose, do not
   self-apply).
2. **Choose or create the right folder.** The placement algorithm in
   [`../rules/directories.md`](../rules/directories.md) governs where the output
   goes — including **creating a new folder when nothing fits**. Never force a
   file into the closest folder and never rename it to pretend it fits.
3. **Write the file.** Valid frontmatter, one topic, imperative and testable
   rules:

   ```
   ---
   name: {kebab-case-unique-id}
   description: {one line, <= 140 chars, routable without opening the body}
   ---

   # {Title}
   ```

4. **Register it** in [`../INDEX.md`](../INDEX.md) — and, if the folder has its
   own child index, there instead — in the same commit. If you created a new
   folder, also add it to the table in
   [`../rules/directories.md`](../rules/directories.md), and register any new
   index in the root `INDEX.md`.
5. **Commit** per the convention below.

## Refusals

* **Refuse to write documentation into `.agents/`.** Project documentation belongs
  in `wiki/` and is the information creator's job
  ([`information-creator.md`](information-creator.md)).
* **Refuse to put rules in `AGENTS.md` or in any `INDEX.md`.** `AGENTS.md` is an
  overview and the indexes are routers. A rule body goes in an instruction file.
* Refuse to place a loose file at the root of `.agents/` — `INDEX.md` is the only
  file allowed there.

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
* Same type list as above. Scope is a module or subsystem, e.g. `docs(agents):`,
  `feat(identity):`, `chore(deps):`.
* Subject in imperative mood, plain text, no trailing period, no links, no
  issue-tracker IDs.
* Optional body: short bullets explaining what and why.
* Commit each logical change or group of related changes — never batch a whole
  session into one commit. Review the diff before every commit.

Example:

```
docs(agents): add the folia compatibility check prompt

- Describe the region-threading violations to look for
- Link the prompt from the knowledge file it supports
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
