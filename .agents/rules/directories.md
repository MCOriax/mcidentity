---
name: directory-architecture
description: Agent directory architecture and strict isolation rules for the .agents/ instruction tree.
---

# Rules: Directory Architecture

Outline the agent directory architecture and strict isolation rules:

* All agent-specific configurations are centralized under `.agents/`. Standard subdirectories include:
  * `.agents/`: Root directory for all agent configurations.
  * `.agents/skills/`: Specific skill definitions and execution steps.
  * `.agents/tools/`: Tool definitions and schemas.
  * `.agents/knowledge/`: Domain-specific context.
  * `.agents/personas/`: Specific roles and behaviors to adopt.
  * `.agents/ethics/`: Safety boundaries and constraints.
  * `.agents/git/`: Version control rules, commit message formats (e.g., Conventional Commits), and branching strategies.
  * `.agents/dependencies/`: Package management policies, external library constraints, and version update rules.
  * `.agents/planning/`: Procedures for analyzing issues, breaking down tasks, estimating effort, and prioritization.
  * `.agents/architecture/`: System design guidelines, design patterns, and architectural constraints.
  * `.agents/api/`: API design standards, endpoint naming conventions, and specification guidelines.
  * `.agents/database/`: Database schema design rules, data migration procedures, and query optimization constraints.
  * `.agents/security/`: Security policies, vulnerability prevention guidelines, and secret scanning rules.
  * `.agents/performance/`: Coding guidelines for optimal performance, memory management, and bottleneck prevention.
  * `.agents/compliance/`: Legal policies, open-source license checking, and data privacy regulations.
  * `.agents/deploy/`: Deployment procedures, environment configurations, and containerization setup.
  * `.agents/workflows/`: CI/CD pipeline automation rules and workflow management constraints.
  * `.agents/prompts/`: Agent prompt templates, system overrides, and few-shot examples for specific tasks.
  * `.agents/docs/`: Rules and templates for generating documentation (e.g., README, Wiki, inline comments).
