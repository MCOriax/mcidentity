---
name: directory-architecture
description: Placement authority for the .agents/ and wiki/ trees, including how to create a new folder when none fits.
---

# Rules: Directory Architecture

This file is the placement authority for both trees. Every new instruction and
every new documentation page is placed by the algorithm in section C.

## A. `.agents/` baseline folders

Use only those this project needs. Each file is `.agents/{folder}/{file}.md`.

| Folder | Holds |
|---|---|
| `rules/` | Repository-wide rules and the directory architecture itself. |
| `git/` | Branching strategy, commit format, pull request format. |
| `creators/` | The instruction / information / changelog / index creator agents. |
| `prompts/` | Standing prompt templates and few-shot examples. |
| `docs/` | Rules for writing README, wiki, and index files. |
| `skills/` | Step-by-step procedures for recurring tasks. |
| `tools/` | Tool definitions and schemas. |
| `knowledge/` | Domain context an agent needs to reason correctly. |
| `personas/` | Roles and behaviors to adopt. |
| `ethics/` | Safety boundaries and constraints. |
| `planning/` | Task intake, breakdown, ordering, estimation, prioritization. |
| `architecture/` | System design guidelines and structural constraints. |
| `api/` | API design standards and specification guidelines. |
| `database/` | Schema design, migrations, query constraints. |
| `security/` | Security policy, secret handling, vulnerability prevention. |
| `performance/` | Performance, memory, and bottleneck guidelines. |
| `dependencies/` | Package management and version-update policy. |
| `compliance/` | Licensing, legal, and privacy policy. |
| `deploy/` | Deployment, environments, containerization. |
| `workflows/` | CI/CD automation rules. |
| `testing/` | Test strategy, coverage, fixtures. |

Every folder in this table that this repository actually uses is listed in
[`../INDEX.md`](../INDEX.md).

## B. `wiki/` baseline folders

Each page is `wiki/{folder}/{file-name}.md`.

| Folder | Holds |
|---|---|
| `information/` | What the project is, architecture, features, concepts. |
| `environments/` | `docker.md`, `docker-compose.md`, `env.md`, local setup, CI. |
| `guides/` | Task-oriented how-tos. |
| `reference/` | Commands, config keys, API surface, schema. |
| `logs/` | Versioned change logs — see [`../creators/changelog-creator.md`](../creators/changelog-creator.md). |

## C. Placement algorithm

1. **Pick the existing folder whose topic actually contains the new file's
   subject.**
2. **If no existing folder fits, do NOT force it into the closest one and do NOT
   rename the file to pretend it fits.** Create a new folder that fits:
   lowercase kebab-case, a plain topic noun (e.g. `.agents/observability/`,
   `wiki/integrations/`), and put the file there.
3. **Whenever you create a new folder, register it in this file's table AND in
   the index that owns that scope, in the same commit.** If the new folder needs
   its own index, register that index in the root `INDEX.md` too. The tables
   above are a baseline, not a closed set.
4. **If an existing file already covers the subject, extend that file** instead of
   adding a near-duplicate — subject to the Discovery Protocol: propose, do not
   self-apply. See
   [`../creators/instruction-creator.md`](../creators/instruction-creator.md).
5. **Never place a loose file at the root of `.agents/` or at the root of
   `wiki/`** (except `INDEX.md`).
6. **Depth is `{tree}/{folder}/{file}.md`.** Go deeper only for `wiki/logs/`,
   which has its own required shape,
   `wiki/logs/{Major}/{Minor}/{Patch}/{file-name}.md`.

## Naming

* All file and folder names under `.agents/` and `wiki/` are **kebab-case**,
  lowercase, no spaces — `instruction-creator.md`, `getting-started.md`.
* `INDEX.md` is the one uppercase exception, everywhere it appears.
* One topic per file. If a file needs two H1-level subjects, it is two files.

## Isolation

* `.agents/INDEX.md` is the **sole authority** that indexes and manages
  `.agents/`. Nothing outside `.agents/` may dictate or write files inside it.
* `wiki/INDEX.md` indexes `wiki/` and must never write into `.agents/`.
* The root `INDEX.md` is a **router only** and must never be used to dictate or
  write files inside any subtree.
