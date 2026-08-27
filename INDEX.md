---
name: root-index
description: Root router — points to every other index in the MCIdentity repository.
---

# Repository Index

| Index | Scope | Load when |
|---|---|---|
| `agents://index/root-index.md` | Shared instruction set (`lxagents-agents-base` connector) | You need a branch, commit, pull request, task workflow, creator, placement, or versioning convention. |
| [`.agents/INDEX.md`](.agents/INDEX.md) | Local agent instruction set | You need a rule, skill, persona, or knowledge file specific to this repository. |
| [`wiki/INDEX.md`](wiki/INDEX.md) | Project documentation | You need to read or write user-facing docs. |
| [`wiki/logs/INDEX.md`](wiki/logs/INDEX.md) | Versioned change logs | You need release history or must record a change. |

This file lists indexes only — never rules, never documentation, never leaf
content links. Adding, removing, or renaming any `INDEX.md` in the repository
updates this table in the same commit. Read exactly one branch per task; do not
preload the others.
