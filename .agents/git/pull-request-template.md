---
name: pull-request-template
description: Human-readable pull request title rules and the required Overview/Added/Modified/Deleted/Summary body skeleton.
---

# Git: Pull Request Template

## Title

A pull request title is written **for a human scanning a list of pull requests**,
not for a parser.

* **Never** use a Conventional Commit prefix in a pull request title — no `feat:`,
  `fix:`, `chore:`, `docs:`, `refactor:`. That convention belongs to commits alone
  ([`commit-conventions.md`](commit-conventions.md)).
* Write a plain, capitalized phrase, verb-first where it reads naturally. No
  trailing period, no issue IDs, no emoji, no branch names.
* Say what the change *is*, specifically. "Update files" is not a title.
* Lead with `Breaking Change` when the pull request breaks an existing contract.

Expected shape:

* `Breaking Change: replace the session token format`
* `Add new API for identity lookup`
* `Delete the legacy migration scripts`
* `Update the Docker environment to Node 22`
* `Fundamental rework of the storage layer`

## Body

Use exactly these sections, in this order:

```
# Overview
{contents}

# Added
- {contents}
- {contents}

# Modified
- {contents}
- {contents}

# Deleted
- {contents}
- {contents}

# Summary
{contents}
```

## Body rules

* **Overview** — two to five sentences: what this pull request does, and why it
  exists. Enough that a reviewer who has not read the task can follow it.
* **Added / Modified / Deleted** — one bullet per real change, each naming the
  file, module, endpoint, or surface it touches, then what changed about it. No
  vague bullets ("various fixes"), no bullet that restates the heading.
* Keep the heading order. Drop a section only when it is genuinely empty — do not
  keep a heading with "None" or "N/A" under it.
* **Summary** — the reviewer's takeaway: blast radius, risk, what to test, and
  what follows this pull request.
* Never leave a `{contents}` placeholder, and never ship an empty section.
* Add the merge-order line required by
  [`../planning/task-workflow.md`](../planning/task-workflow.md) when the pull
  request is part of an ordered chain, e.g.
  `Merge order: 2 of 4 — merges after #123`.
* Call out the blast radius this repository cares about: whether the change
  touches `bukkit` schedulers or any `platforms:*` module (engine / cross-platform
  adjustment) or is confined to `api` and `common` (pure API addition). See
  [`branching-strategy.md`](branching-strategy.md).
* If the repository also has a `.github/pull_request_template.md`, this file stays
  the source of truth for wording; mirror these headings there rather than
  maintaining two different shapes.
