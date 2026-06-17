# Global Instructions & Skills for AI Agents

All agents (Windsurf, Claude Code, Codex, and others) MUST read and adhere to the latest instructions from the local repository before and during execution.  
This project utilizes a centralized instruction and skill management system to maintain consistency across all AI platforms.

**CRITICAL INSTRUCTION:**
This file acts as the primary instruction set, and any specific rules found within the `.agents/` folder at the root of this repository override general knowledge.

You MUST immediately read the central routing hubs to understand the project structure and locate the relevant skills for your current session:

* 👉 **Central Index & Routing Hub:** .agents/README.md
* 👉 **Agent Skills Target File:** .agents/skills/README.md

Proceed to read the target files above before executing any tasks or modifying any code.

Don't reference pull requests or issues with `#` in commit messages. Commit messages must be plain and contain no links.

When updating code, you must check whether the test code also needs to be updated. Always run the tests and the build; if there are any errors, you must fix them before opening a pull request.
