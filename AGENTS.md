---
name: agents-entry-point
description: Universal entry point for agents working in the MCIdentity repository — reading order, routing protocol, and standing conventions.
---

# AGENTS.md

This repository is the **MCIdentity Minecraft plugin**: a Gradle multi-project
build (`api`, `common`, `bukkit`, `platforms:*`) that ships identity, profession,
and stat features across SpigotMC, PaperMC, and Folia. Skills, runes, and
cosmetics live in separate add-on repositories.

## Reading order (mandatory)

1. Read `AGENTS.md`.
2. Read the root [`INDEX.md`](INDEX.md) — and nothing else at this stage.
3. From its routing table, pick the ONE index whose scope matches the task, and
   read that index.
4. If that index delegates to a child index, follow the one branch that matches.
5. Only then open the specific file(s) you need.

## Routing protocol (context discipline)

Route by reading index tables, not by reading files.

* Do NOT load every `INDEX.md`.
* Do NOT bulk-scan `.agents/**` to build a registry.
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
  [`.agents/rules/directories.md`](.agents/rules/directories.md).
* New documentation goes to `wiki/{folder}/{file}.md`.
* New or updated indexes follow
  [`.agents/creators/index-creator.md`](.agents/creators/index-creator.md).

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

## Standing conventions

Always active. The user never has to restate them.

| Convention | Authority |
|---|---|
| Task intake, decomposition, branch order, merge order | [`.agents/planning/task-workflow.md`](.agents/planning/task-workflow.md) |
| Branch naming | [`.agents/git/branching-strategy.md`](.agents/git/branching-strategy.md) |
| Commit messages | [`.agents/git/commit-conventions.md`](.agents/git/commit-conventions.md) |
| Pull request title and body | [`.agents/git/pull-request-template.md`](.agents/git/pull-request-template.md) |
| The standing prompt tying them together | [`.agents/prompts/branch-and-commit.md`](.agents/prompts/branch-and-commit.md) |

## Version rule

Never change the project version without explicit user approval — see
[`.agents/rules/versioning.md`](.agents/rules/versioning.md). This includes
creating a new `wiki/logs/{Major}/{Minor}/{Patch}/` directory, which is itself a
version claim.
