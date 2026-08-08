---
name: branch-and-commit-prompt
description: Standing prompt applying the branch, commit, and pull request convention to every task without being restated.
---

# Prompt: Branch and Commit

**This applies to every task in this repository. Assume it is always active; the
user does not need to repeat it.**

## The loop

1. Confirm **Goal / Objective / Detail** with the user
   ([`../planning/task-workflow.md`](../planning/task-workflow.md), part A).
2. Split the request into ordered tasks `1…n` and get the list confirmed
   (part B). A self-contained request stays one task.
3. Create one branch per task, stacked in order — task 1 off the default branch,
   task `k` off task `k-1` (part C).
4. Work tasks `1…n` strictly in order; finish and commit task `k` before starting
   `k+1` (part D).
5. Update the index that owns the scope for anything added, removed, moved, or
   renamed — in the same commit
   ([`../creators/index-creator.md`](../creators/index-creator.md)).
6. Commit each logical change with `type(scope): description`.
7. Push every branch with `git push -u origin {branch}`.
8. Open one pull request per branch using
   [`../git/pull-request-template.md`](../git/pull-request-template.md).
9. **Ask the user before merging.** Wait for an explicit yes.
10. Merge in order `1…n` (part E).

## Branch naming

* Format: `{type}/{primary-noun}` — e.g. `feat/login`, `fix/schema-drift`,
  `docs/agents-setup`.
* Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
  `build`, `ci`, `chore`, `revert`.
* **Never commit to the default branch, and never use a tool-preset branch prefix**
  such as `claude/`, `codex/`, or `cursor/`. If a branch violates the convention,
  recreate it correctly and delete the wrong one, or present the options to the
  user.
* Keep branches short-lived and rebased on the default branch.

## Commit messages

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
feat(database): add PostgreSQL backend

- Implement IMCIdentityDB via AbstractSqlDB
- Register the type in DatabaseFactory
```

## Pull request titles

**Pull request titles are human-readable, never `feat:` / `fix:` / `chore:`.**
Write a plain capitalized phrase that says what the change is, e.g.
`Add PostgreSQL storage backend`. Full rules in
[`../git/pull-request-template.md`](../git/pull-request-template.md).

## Authority

[`../planning/task-workflow.md`](../planning/task-workflow.md) is the authority
for intake, task ordering, branch stacking, and merging. This prompt is the
checklist; that file is the rule.
