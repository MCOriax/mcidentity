---
name: shared-instruction-adoption
description: Record of the duplicate-instruction audit against the lxagents-agents-base connector and what it removed.
status: done
---

# Task: Shared Instruction Adoption

## 2026-08-27

Ran the duplicate-instruction audit against the `lxagents-agents-base` connector
(shared set version 0.10.0, 26 files) at the user's request.

**Found:** 11 stale copies, matched by frontmatter `name` — the override key.
Every body differed from the shared file, and the repository declares no override
table, so all 11 were undeclared drift rather than deliberate overrides.

| Removed | Shadowed |
|---|---|
| `.agents/creators/changelog-creator.md` | `agents://creators/changelog-creator.md` |
| `.agents/creators/index-creator.md` | `agents://creators/index-creator.md` |
| `.agents/creators/information-creator.md` | `agents://creators/information-creator.md` |
| `.agents/creators/instruction-creator.md` | `agents://creators/instruction-creator.md` |
| `.agents/git/branching-strategy.md` | `agents://git/branching-strategy.md` |
| `.agents/git/commit-conventions.md` | `agents://git/commit-conventions.md` |
| `.agents/git/pull-request-template.md` | `agents://git/pull-request-template.md` |
| `.agents/planning/task-workflow.md` | `agents://planning/task-workflow.md` |
| `.agents/prompts/branch-and-commit.md` | `agents://prompts/branch-and-commit.md` |
| `.agents/rules/directories.md` | `agents://rules/directories.md` |
| `.agents/rules/versioning.md` | `agents://rules/versioning.md` |

**Kept:** 28 local-only instruction files. `.agents/INDEX.md` and
`.agents/rules/repository.md` are excluded from the audit by procedure.

**Behaviour this changes.** The local `task-workflow` was the file in force until
now, and it had no reserved slots. The shared one reserves task 1 for the task
record in `.agents/memory/tasks/` and task `n` for the release, so a future
request yields at least three tasks.

**Local content rescued rather than dropped.** Three of the deleted files carried
rules the shared set does not have. All three were repository-specific, so they
moved into `.agents/rules/repository.md` — a local-only file with no `name`
collision — rather than being promoted upstream or kept as an override:

* the engine / pure-API blast-radius call-out, from the local `branching-strategy`
  and `pull-request-template`;
* "verify means the Gradle build and tests pass for the modules you touched", from
  the local `task-workflow` §D;
* the version-carrier table naming `gradle.properties` and the `plugin.yml` files,
  from the local `versioning`.

**Still open.** This repository's index is `.agents/INDEX.md`, not the
`.agents/index/{scope}-index.md` layout the shared set now assumes, so there is no
`root-index.md` override table and no `memory-index.md`. The memory file above is
registered in `.agents/INDEX.md` instead. Migrating the index layout is the
`agents-setup` prompt's job and was deliberately left out of the audit.

**Branch:** `chore/shared-instruction-adoption`.
