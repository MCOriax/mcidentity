# Global Instructions & Skills for AI Agents

All agents (Windsurf, Claude Code, Codex, and others) MUST read and adhere to
the latest instructions in this file before and during execution. This project
uses a centralized instruction and skill management system to maintain
consistency across all AI platforms.

**CRITICAL INSTRUCTION:** This file is the primary instruction set. Any specific
rules it contains override general knowledge. Project-specific skills live under
`.agents/skills/` and are listed in the [AI Agent Skills Index](#ai-agent-skills-index)
below.

## Architecture & Technical Standards

### Project Structure

Maintain the sub-module architecture:

- `api`
- `common`
- `bukkit`
- `platform`

Additional rules:

- `api` and `common` must ALWAYS be pure Java and use `CompletableFuture`.
- `api` submodule must contain ONLY interfaces.
- `common` submodule is strictly for getting and setting data from the Database.
- `bukkit` submodule is dedicated to handling core event structures and API interactions specific to the Bukkit platform.
- `platforms` submodule is dedicated to handling platform-specific implementations and integrations.

## Core Directives

- **File Allocation**: Create specific `.md` files grouped within dedicated folders for related topics (e.g., `.agents/database/task-database.md`, `.agents/issues/bug-reports.md`). Structure general files as `.agents/{folder name}/{file name}.md` and skill-specific files as `.agents/skills/{folder name}/{file name}.md`. Do not dump all information into a single file. Read only the specific files relevant to the current objective.
- **Testing & Build**: When updating code, you must check whether the test code also needs to be updated. Always run the tests and the build; if there are any errors, you must fix them before opening a pull request.

## Code Style

- Never use full paths in code; always use imports.
- Provide code blocks with English comments only.

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

- **Branch Isolation (CRITICAL)**: NEVER work directly in the `master` branch. If the current branch is `master`, checkout a new branch immediately before making modifications.
- **Task Scoping**: If assigned a task unrelated to the current branch, create a new branch specific to that task.
- **Branch Naming (STRICT)**: Must strictly follow the `{type}/{primary-noun}` or `{type}/{primary-noun}-{secondary-noun}` format. Do not use verbs or Jira IDs.
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
- **Branch Check Before Working**: Before starting any work, always check the current branch. You must never work directly in the `master` or `main` branch.
- **Iteration Bump for Java Submodules**: When updating code within a Java submodule, you must always increment `project-iteration` in `gradle.properties` by 1. If the update does not change any code that affects the plugin itself, you must not increment `project-iteration`.

## AI Agent Skills Index

This project provides specific technical skills, domain knowledge, and coding
standards required for this project. Always refer to the relevant skill file
before modifying specific components.

### Skill Categories

*   **Platforms**
    *   `.agents/skills/spigotmc.md`: Senior Spigot/Bukkit plugin developer — maximum compatibility, main-thread safety, Bukkit API first.
    *   `.agents/skills/papermc.md`: Senior Paper plugin developer — richer Paper API, Adventure Components, async chunk and Brigadier APIs.
    *   `.agents/skills/foliamc.md`: Senior Folia plugin developer — regionized threading, correct scheduler per thread context, strict ownership and concurrency rules.
