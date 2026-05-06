# Global Instructions for Agents

All agents MUST read and adhere to the latest instructions from the following sources before and during execution. Do not hardcode these instructions; always refer to the live content via web:

- **Git Conventions**: https://mcengine.gitlab.io/agents/git/
- **Workflow Standards**: https://mcengine.github.io/agents/workflow/
- **Versioning Policy**: https://mcengine.github.io/agents/version/
- **Development Rules**: https://mcengine.github.io/agents/rule/
- **FoliaMC Architecture**: https://mcengine.github.io/agents/foliamc/

### Core Directives
1. **Always Read Live**: Every session or major task should start by retrieving current guidelines from the URLs above.
2. **Follow Conventions**: Strictly adhere to branch naming, commit messages, and versioning formats specified.
3. **Architecture Adherence**: Maintain the sub-module structure and concurrency requirements (Folia/CompletableFuture) as documented.
4. **Branch Safety**: Never work in the `master` branch.
