# Global Instructions for AI Agents

All agents (Windsurf, Claude Code, Codex, and others) MUST read and adhere to the latest instructions from the following sources before and during execution. Do not rely on internal training data or hardcode these instructions; always fetch the live content via web browser tools.

## 🌐 Live Documentation Sources
- **Git Conventions**: https://mcengine.gitlab.io/agents/git/
- **Workflow Standards**: https://mcengine.github.io/agents/workflow/
- **Versioning Policy**: https://mcengine.github.io/agents/version/
- **Development Rules**: https://mcengine.github.io/agents/rule/
- **FoliaMC Architecture**: https://mcengine.github.io/agents/foliamc/

## 🛠 Core Directives

### 1. Mandatory Live Retrieval
- Every session or major task MUST start by retrieving current guidelines from the URLs listed above.
- If the agent has a "Browser" or "Web Search" tool, it must be used to verify the content of these links before writing code.

### 2. Versioning & Git Workflow
- **Branch Safety**: NEVER work directly in the `master` branch.
- **Branch Naming**: All new branches must follow the format `{type}/{primary-noun}` (e.g., `feat/api-interface`).
- **Task Scoping & Branching**: If the user assigns a task that is unrelated to the current branch, agents must create a new branch specific to that task.
- **Pull Requests (PR)**: Pull Requests must be opened sequentially in the correct order. The agent must always ask the user for permission before creating a PR.
- **Commit Messages**: Strictly adhere to **Conventional Commits** (feat, fix, BREAKING CHANGE).
- **Versioning**: Apply the 2026 iterative versioning strategy (e.g., 2026.1.x) as documented in the versioning policy.
- **Commit Frequency**: Commit each change or group commits if related; do not wait for the entire session to finish.
- **Diff Verification**: Always check the diff before creating a commit.

### 3. Technical Standards (MCEngine)
- **Project Structure**: Maintain the sub-module architecture as defined (api, common, platform).
- **Concurrency**: Adhere to FoliaMC requirements, utilizing `CompletableFuture` and proper regionalized threading.
- **Code Style**: Never use full paths in code; always use imports.
- **Documentation**: Provide code blocks with **English comments only** while responding in **Thai**.

### 4. Credentials & Security
- **CI/CD**: Prioritize GitLab CI/CD environment variables (`CI_REGISTRY_USER`, `CI_JOB_TOKEN`).
- **Data Access**: If a task involves a specific organization, refer to `agents/orgs.md` in Google Drive to identify its category.
- **Source Code**: Check both GitHub and GitLab for repositories as they are separate entities.

---
**Note**: This file acts as the primary instruction set. Any specific rules found within the `agents/` folder at the root of this repository override general knowledge.
---
