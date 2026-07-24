# AGENTS.md

Universal entry point and reference for all agents working in this repository.

## Reading Order (MANDATORY)

* Always READ `INDEX.md` first, and then go read `.agents/INDEX.md`.
* When evaluating or operating on `AGENTS.md`, chain the reading process explicitly: first read the root `INDEX.md`, then read `.agents/INDEX.md`.

## Iron Rules (Separation of Concerns)

* IRON RULE: The root `INDEX.md` must NEVER be used to dictate or write files inside the `.agents/` directory.
* The root `INDEX.md` must NEVER write to, manage, or modify any files within the `.agents/` directory. Management of `.agents/` is strictly delegated to `.agents/INDEX.md`.

These rules persist the separation of concerns so it is recognized by all future sessions:

* Root `INDEX.md` — indexes and describes the repository's own source, docs, and tooling only.
* `.agents/INDEX.md` — the sole authority that indexes and manages the `.agents/` instruction set.
