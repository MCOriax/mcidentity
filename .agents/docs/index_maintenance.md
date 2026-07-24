---
name: index-maintenance
description: Mandate to keep the root INDEX.md current with repository structure changes.
---

# Docs: Index Maintenance

## Mandate

* Immediately update `INDEX.md` when any new file or module is added to the
  repository root, or when the structure/indexed content changes.
* `INDEX.md` reflects only the files and directories that exist in this
  repository.

## Rules

* Every new module, package, or top-level directory gets its own row/section.
* Keep links relative and clickable.
* Separation of concerns (see root `AGENTS.md`): the root `INDEX.md` indexes the
  repository's own source, docs, and tooling. It must NEVER be used to dictate or
  write files inside `.agents/` — that is delegated to `.agents/INDEX.md`.
* When agent instructions change, update `.agents/INDEX.md`, not the root
  `INDEX.md`.
