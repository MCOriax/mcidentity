# Global Instructions for AI Agents

All agents (Windsurf, Claude Code, Codex, and others) MUST read and adhere to the latest instructions from the local repository before and during execution.

**Note**: This file acts as the primary instruction set. Any specific rules found within the `.agents/` folder at the root of this repository override general knowledge.

---

## 1. Core Directives & Local State Management

- **Mandatory Instruction Retrieval**: Every session or major task MUST start by retrieving and reading the latest instructions from this file and related files inside the repository. Do not rely on internal training data or hardcoded assumptions.
- **Local State Directory (`.agents/`)**: Store all active work, session context, and task summaries in the local `.agents/` directory.
  - **Gitignore Verification**: Verify that `.agents/` is present in the root `.gitignore` file. If not, write it immediately to prevent accidental commits.
  - **Central Index**: Maintain `.agents/README.md` as the routing hub with brief descriptions of all specific files. Always read this file first to locate the context of a new session.
  - **File Allocation**: Create specific `.md` files for related topics (e.g., `.agents/task-database.md`, `.agents/bug-reports.md`). Do not dump all information into a single file. Read only the specific files relevant to the current objective.

---

## 2. Git & Branching Workflow

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

---

## 3. Commit Message Conventions (STRICT)

Commits MUST follow the Conventional Commits specification.

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

---

## 4. Versioning Specification & CI/CD

### Release & CI Versioning

#### Format Architecture

- **CI Builds**:
  ```text
  2026.m.m-build.${build number}
  ```

- **Stable Tests**:
  ```text
  2026.m.m-{iteration 1...n}
  ```

- **Releases**:
  ```text
  2026.m.m
  ```

### Timeline

Releases update iteratively per month (e.g., `2026.1.0` in Oct, `2026.1.1` in Nov). In December, prepare the New Year format.

### Iteration Workflow

The first commit after creating a branch MUST bump the `project-iteration` by `1` using the formula:

```text
{current iteration} + 1
```

in `gradle.properties`.

### gradle.properties Standard

Must ALWAYS provide these 5 variables:

```properties
project-group=
git-org-name=
git-repository=
project-version=
project-iteration=
```

### Credentials & Security

Prioritize GitLab CI/CD environment variables, followed by Agent-specific, then User-specific credentials.

Always check both GitHub and GitLab for repositories as they are separate entities.

```yaml
USERNAME = System.getenv('CI_REGISTRY_USER') ?: System.getenv('AGENT_GITLAB_NAME') ?: System.getenv('USER_GITLAB_NAME')
PASSWORD = System.getenv('CI_JOB_TOKEN') ?: System.getenv('AGENT_GITLAB_TOKEN') ?: System.getenv('USER_GITLAB_TOKEN')
```

---

## 5. Architecture & Technical Standards

### MCEngine Project Structure

Maintain the sub-module architecture:

- `api`
- `common`
- `platform`

Additional rules:

- `api` and `common` must ALWAYS be pure Java and use `CompletableFuture`.
- `api` submodule must contain ONLY interfaces.
- `common` submodule is strictly for getting and setting data from the Database.

### FoliaMC Concurrency & Regions

- Strictly utilize concurrent programming and respect Region constraints.
- Use Global Threading Entity Schedules for all entity-related tasks.
- ALWAYS use `Component.translatable()` for player messages (similar to PaperMC).

### Code Style

- Never use full paths in code; always use imports.
- Provide code blocks with English comments only.
