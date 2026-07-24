---
name: agents-entry-point
description: Universal entry point and mandatory reading order for all agents working in the MCIdentity repository.
---

# AGENTS.md

Universal entry point and reference for all agents working in this repository.

## Reading Order (MANDATORY)

* Always READ `INDEX.md` first, and then go read `.agents/INDEX.md`.
* When evaluating or operating on `AGENTS.md`, chain the reading process explicitly: first read the root `INDEX.md`, then read `.agents/INDEX.md`.

## Agent Initialization & Routing Protocol

1. **Memory Pre-loading (Registry):** When evaluating tasks or initializing, read ONLY the YAML frontmatter (`name` and `description`) from all Agent `.md` files into memory to build an Agent Registry.
2. **Prevent Context Overload:** Do NOT read or load the full instruction body of any Agent's `.md` file during the initial routing or evaluation phase.
3. **Lazy Loading:** Analyze the user's request against the descriptions in the memory registry. Once a specific Agent is selected for the task, only then load its full instruction context from its respective `.md` file.

## Iron Rules (Separation of Concerns)

* IRON RULE: The root `INDEX.md` must NEVER be used to dictate or write files inside the `.agents/` directory.
* The root `INDEX.md` must NEVER write to, manage, or modify any files within the `.agents/` directory. Management of `.agents/` is strictly delegated to `.agents/INDEX.md`.

These rules persist the separation of concerns so it is recognized by all future sessions:

* Root `INDEX.md` — indexes and describes the repository's own source, docs, and tooling only.
* `.agents/INDEX.md` — the sole authority that indexes and manages the `.agents/` instruction set.

## Post-Work Discovery Protocol

* After finishing the work, if an agent — while using the full agent instructions — discovers that content should be added to an existing file or that a new file should be created, first present to the user everything it found, including the proposed `name`, `description`, and details.
* If there are multiple instructions, the agent must present each one in its own code block.
* Let the user choose which instruction to apply before creating anything.
* Agents must create any new file following the format described in [`.agents/rules/directories.md`](.agents/rules/directories.md).
