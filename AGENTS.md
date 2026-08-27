---
name: agents-entry-point
description: Universal entry point for agents working in the MCIdentity repository — shared set resolution, reading order, and the auto-activation trigger table.
---

# AGENTS.md

This repository is the **MCIdentity Minecraft plugin**: a Gradle multi-project
build (`api`, `common`, `bukkit`, `platforms:*`) that ships identity, profession,
and stat features across SpigotMC, PaperMC, and Folia. Skills, runes, and
cosmetics live in separate add-on repositories.

## Shared Instruction Set

The conventions this repository follows — branching, commits, pull requests, task
workflow, the creators — live in the shared instruction set served by the
**`lxagents-agents-base`** MCP server. This repository carries only what is its
own. **Resolve the shared set before doing any work:**

1. If the `lxagents-agents-base` connector is available in this session, that is
   the shared set. Refer to it as `{shared}`; its files are addressed as
   `agents://{folder}/{file}.md`.
2. Read `agents://manifest.json` once. It lists every shared file with its `name`,
   path and description — one read instead of twenty, and it is what the routing
   tables below are checked against.
3. Read `agents://index/root-index.md` and route from there. Do not bulk-read the
   set.
4. If the connector is not available, say so plainly and continue with this
   repository's local instruction set only. **Do not reconstruct the missing rules
   from memory, and do not clone or copy them into this repository.**

Never commit shared content into this repository. A file that can be read from
`agents://` must not exist here as a copy — see
`{shared}/rules/duplicate-instruction-audit.md`.

**Local overrides shared.** A file in `.agents/` whose `name` matches a shared
file's `name` replaces that shared file entirely for this repository.
**This repository currently declares no overrides.** Its index still sits at
[`.agents/INDEX.md`](.agents/INDEX.md) rather than `.agents/index/root-index.md`;
migrating to the `index/` layout is the `agents-setup` prompt's job, not this
file's.

## Reading order (mandatory)

1. Read `AGENTS.md`.
2. Resolve the shared set, as above.
3. Read the root [`INDEX.md`](INDEX.md) — and nothing else at this stage.
4. From its routing table, pick the ONE index whose scope matches the task, and
   read that index.
5. If that index delegates to a child index, follow the one branch that matches.
6. Only then open the specific file(s) you need.

## Routing protocol (context discipline)

Route by reading index tables, not by reading files.

* Do NOT load every `INDEX.md`.
* Do NOT bulk-scan `.agents/**` or the shared set to build a registry.
* Do NOT read an instruction body until that instruction has been selected.

Each index row's purpose text is what you route on; the file body is what you load
after choosing. This is the whole point of the index tree — never defeat it by
reading ahead.

## Iron rule (separation of concerns)

* `AGENTS.md` and `README.md` are overviews and must never carry detailed rules
  or documentation.
* The root `INDEX.md` is a **router only**. It lists other indexes. It must never
  contain rules, documentation, prose, or direct links to leaf content, and it
  must never be used to dictate or write files inside any subtree.
* `.agents/INDEX.md` is the *sole* authority that indexes and manages `.agents/`;
  nothing outside `.agents/` may dictate or write files inside it.
* `wiki/INDEX.md` indexes `wiki/` and must never write into `.agents/`.

## Placement

* New instructions go to `.agents/{folder}/{file}.md` per
  `{shared}/rules/directories.md`.
* New documentation goes to `wiki/{folder}/{file}.md`.
* New or updated indexes follow `{shared}/creators/index-creator.md`.

## Auto-activation trigger table

Mirrored row for row from `{shared}/rules/auto-activation.md`, which is the source
of truth, with this repository's own rows appended below.

| When you are about to… | Load and obey |
|---|---|
| Take in any new request of more than one step | `{shared}/planning/task-workflow.md` |
| Create a branch | `{shared}/git/branching-strategy.md` |
| Write a commit message | `{shared}/git/commit-conventions.md` |
| Open or update a pull request | `{shared}/git/pull-request-template.md` |
| Write **any** commit, tag, PR, comment, or file that will be committed or posted | `{shared}/rules/no-session-links.md` |
| Wonder whether something is local or shared, or need to override a shared rule | `{shared}/rules/shared-instructions.md` |
| Decide where a new file goes | `{shared}/rules/directories.md` |
| Resolve, connect, or fail to reach the shared set | `{shared}/rules/mcp-connector.md` |
| Add, move, rename, or delete any file in a set or in `wiki/` | `{shared}/creators/index-creator.md` |
| Write a rule or instruction | `{shared}/creators/instruction-creator.md` |
| Write documentation, an SOP, or a domain guideline | `{shared}/creators/information-creator.md` |
| Change code or structure that a document describes | `{shared}/rules/change-propagation.md` |
| Record progress, a decision, or session state | `{shared}/creators/memory-creator.md` |
| Touch anything that carries a version number | `{shared}/rules/versioning.md` |
| Record a release | `{shared}/creators/changelog-creator.md` |
| Report finished work back to the user | `{shared}/rules/work-summary.md` |
| Need project facts, commands, or orientation | `{repo}/.agents/wiki/context/repository-map.md` |
| Do anything at all in this project | `{repo}/.agents/rules/repository.md` |

`.agents/wiki/` does not exist in this repository yet; until it does, project
facts and orientation live in [`wiki/information/`](wiki/information/) and in
[`.agents/rules/repository.md`](.agents/rules/repository.md).

### This repository's own rows

| When you are about to… | Load and obey |
|---|---|
| Write a SQL query or touch a stat identifier | [`.agents/database/sql-injection-prevention.md`](.agents/database/sql-injection-prevention.md) |
| Change the database schema | [`.agents/database/schema-migrations.md`](.agents/database/schema-migrations.md) |
| Add or change a storage backend | [`.agents/architecture/storage-abstraction.md`](.agents/architecture/storage-abstraction.md) |
| Import a type across module boundaries | [`.agents/architecture/module-isolation.md`](.agents/architecture/module-isolation.md) |
| Schedule work, or touch a listener or task | [`.agents/knowledge/folia-threading-model.md`](.agents/knowledge/folia-threading-model.md) and [`.agents/performance/async-first-policy.md`](.agents/performance/async-first-policy.md) |
| Read or write the session cache | [`.agents/performance/ram-caching-rules.md`](.agents/performance/ram-caching-rules.md) and [`.agents/knowledge/identity-data-model.md`](.agents/knowledge/identity-data-model.md) |
| Call the central server | [`.agents/api/central-server-rest-specs.md`](.agents/api/central-server-rest-specs.md) |
| Add a dependency or change the build | [`.agents/dependencies/gradle-multi-project-rules.md`](.agents/dependencies/gradle-multi-project-rules.md) and [`.agents/dependencies/java-25-toolchain.md`](.agents/dependencies/java-25-toolchain.md) |
| Publish or export a jar | [`.agents/deploy/github-packages-publishing.md`](.agents/deploy/github-packages-publishing.md) and [`.agents/deploy/engine-jar-export.md`](.agents/deploy/engine-jar-export.md) |
| Plan a feature bottom-up | [`.agents/planning/feature-implementation-steps.md`](.agents/planning/feature-implementation-steps.md) |
| Act on a SonarCloud report | [`.agents/prompts/sonarqube-remediation.md`](.agents/prompts/sonarqube-remediation.md) |
| Handle a token, a secret, or a serialized blob | [`.agents/security/secret-management.md`](.agents/security/secret-management.md) and [`.agents/security/insecure-deserialization.md`](.agents/security/insecure-deserialization.md) |

## Discovery Protocol

While working, if you notice an instruction worth adding — a new rule, or new
content for an existing instruction file — do NOT create or edit it yourself.
Collect the findings, and when the task is done present them to the user:

* one finding per message block, each in its own code block;
* state the target set — `local` (this repository) or `shared` (the organization's
  instruction set served by the `lxagents-agents-base` connector);
* include the proposed file path, `name`, `description`, and the full proposed
  body;
* explain in one line why it is worth adding.

Then let the user select which findings to apply. Create only the selected ones.
Never batch-apply, never apply silently. A `shared` finding is never written from a
consuming repository — it is reported so it can be raised against the shared set.

**Scope of this gate:** it covers instruction files in either set. Documentation
pages under `wiki/` and `.agents/wiki/` may be written when the facts are real and
verified. Memory under `.agents/memory/` is written freely and automatically — see
`memory-policy.md`.

## Version rule

Never change the project version without explicit user approval — see
`{shared}/rules/versioning.md`. This includes creating a new
`wiki/logs/{Major}/{Minor}/{Patch}/` directory, which is itself a version claim.
