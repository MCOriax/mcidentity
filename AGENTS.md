# Global Instructions & Skills for AI Agents

All agents (Windsurf, Claude Code, Codex, and others) MUST read and adhere to
the latest instructions in this file before and during execution. This project
uses a centralized instruction and skill management system to maintain
consistency across all AI platforms.

**CRITICAL INSTRUCTION:** Read [`INDEX.md`](INDEX.md) first to understand the
project structure, then read this file. `AGENTS.md` is the primary instruction
set; any specific rules it contains override general knowledge. The universal
project skill lives in [`SKILLS.md`](SKILLS.md) at the repository root;
platform specific skills live under `.skills/` and are indexed in
[`.skills/README.md`](.skills/README.md).

## Strict Rules

- Read [`INDEX.md`](INDEX.md) first to understand the project structure. Each
  repository must have its own `INDEX.md`, and its contents must strictly
  reflect only the files and directories of that specific repository. If
  `INDEX.md` is missing, create it. Actively write, edit, update, and fix
  `INDEX.md` whenever a structural or content change in the repository requires
  re indexing.
- Read `AGENTS.md`, `SKILLS.md`, and every `README.md` located within the
  documented Agent Directories, and hold their entire contents in memory while
  working.
- Ask for the user's approval before editing `AGENTS.md` or `SKILLS.md`.
- If `AGENTS.md` and `SKILLS.md` are missing from the root repository, ask the
  user whether to create them. When generating them, make the contents
  accurately match the repository using the context from this file.
- When working with multiple repositories, create an `AGENTS.md` and a
  `SKILLS.md` for each repository. If rules conflict between them, ask the user
  for clarification.
- Never work directly on the `master` or `main` branch. See
  [Git & Branching Workflow](#git--branching-workflow) for the full branch
  rules.
- When finishing work, run a code security check over the files that were
  modified and committed on the current branch.

The root `AGENTS.md` and `SKILLS.md` files must remain universally applicable to
the entire repository. If you conceptualize a new instruction, skill, tool, or
any context specific data that is not universally applicable, create it within
the appropriate Agent Directory (such as `.agents/`, `.skills/`, `.tools/`,
`.personas/`, `.ethics/`) instead.

## Agent Directories

| Name | Description |
|---|---|
| `.agents/` | Context specific agent instructions and workflows. |
| `.skills/` | Platform specific or component specific agent skills. |
| `.tools/` | Definitions, schemas, or scripts of tools the agents are permitted to use. |
| `.knowledge/` | Domain specific context, reference documents, or knowledge base for the agent. |
| `.personas/` | Specific roles, personalities, or perspectives for agents to adopt. |
| `.ethics/` | Safety bounds, ethical guidelines, and constraints for responsible agent behaviour. |

### Directory Structure

- `.agents/{type}/{name}.md`
- `.skills/{type}/{name}.md`

### Indexing Rules

- `INDEX.md` is the single project structure index. Read it first and keep it
  current.
- Every agent related directory (for example `.agents/`, `.skills/`, `.tools/`)
  must contain a `README.md` that serves as the index for its own contents.
- Do not index the contents of agent directories in the root `AGENTS.md` or
  `SKILLS.md`; their `README.md` files own that indexing.

### Suggesting New Content

While working, if you discover a useful idea, rule, or skill that could improve
`AGENTS.md` or `SKILLS.md`, do not apply the changes immediately. Instead, upon
finishing your current task, suggest the new additions to the user and allow them
to decide whether to integrate them.

## Architecture & Technical Standards

### Project Structure

Maintain the sub-module architecture:

- `api`
- `common`
- `bukkit`
- `platforms`

Additional rules:

- `api` and `common` must ALWAYS be pure Java and use `CompletableFuture`.
- `api` submodule must contain ONLY interfaces.
- `common` submodule is strictly for getting and setting data from the Database.
- `bukkit` submodule is dedicated to handling core event structures and API interactions specific to the Bukkit platform.
- `platforms` submodule is dedicated to handling platform-specific implementations and integrations.

## Core Directives

- **File Allocation**: Create specific `.md` files grouped within dedicated folders for related topics (e.g., `.agents/database/task-database.md`, `.agents/issues/bug-reports.md`). Structure general agent files as `.agents/{folder name}/{file name}.md` and skill-specific files as `.skills/{folder name}/{file name}.md`. Do not dump all information into a single file. Read only the specific files relevant to the current objective.
- **Testing & Build**: When updating code, you must check whether the test code also needs to be updated. Always run the tests and the build; if there are any errors, you must fix them before opening a pull request.

## Code Style

- Never use full paths in code; always use imports.
- Provide code blocks with English comments only.
- Code and comments must not contain hyphens or minus signs unless strictly required by syntax.

## Commit Message Conventions (STRICT)

Commits MUST follow the Conventional Commits specification. Commit messages must
be plain and contain no links; do not reference pull requests or issues with `#`.

### Structure

```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types

- `fix:` — Patches a bug (PATCH).
- `feat:` — Introduces a new feature (MINOR).
- `BREAKING CHANGE:` — Introduces a breaking API change (MAJOR).
- Other supported types:
  - `build:`
  - `chore:`
  - `ci:`
  - `docs:`
  - `style:`
  - `refactor:`
  - `perf:`
  - `test:`

### Examples

```text
feat(api)!: send an email to the customer when a product is shipped
```

```text
fix: prevent racing of requests
```

## Git & Branching Workflow

- **Branch Isolation (CRITICAL)**: NEVER work directly in the `master` or `main` branch. If the current branch is `master`, checkout a new branch immediately before making modifications.
- **Task Scoping**: When you start work from a user prompt, check whether it relates to the current branch. If the task is unrelated, create a new branch specific to it. If the work stays within the current scope — for example, adding a new function or fixing an error in existing work — do not create a new branch; continue on the current one.
- **Branch Naming (STRICT)**: Must strictly follow the `{type}/{primary-noun}` or `{type}/{primary-noun}-{secondary-noun}` format. Do not use verbs or Jira IDs. Do not use the `claude/` prefix.
  - **Allowed Types**:
    - `feat/`
    - `fix/`
    - `docs/`
    - `style/`
    - `refactor/`
    - `perf/`
    - `test/`
    - `build/`
    - `ci/`
    - `chore/`
    - `revert/`
- **Commit Frequency & Verification**: Commit each change or group related commits. Do not wait for the entire session to finish. Always check the diff before creating a commit.
- **Pull Requests (PR)**: PRs must be opened sequentially in the correct order. Always ask the user for permission before creating a PR.
- **Iteration Bump for Java Submodules**: When updating code within a Java submodule, you must always increment `project-iteration` in `gradle.properties` by 1. If the update does not change any code that affects the plugin itself, you must not increment `project-iteration`.
