---
name: agents-index
description: Index of .agents/ — the sole authority for the MCIdentity agent instruction set.
---

# Agents Index

**Scope:** `.agents/`
**Parent:** [root-index](../INDEX.md)

This index is the sole authority that indexes and manages `.agents/`. Nothing
outside `.agents/` may dictate or write files inside this tree. Any file added to
or removed from `.agents/` is reflected here in the same commit.

## Rules

| File | Purpose |
|---|---|
| [`rules/repository.md`](rules/repository.md) | Rules specific to this repository: module boundaries, storage facade, build commands. |
| [`rules/directories.md`](rules/directories.md) | Placement authority for `.agents/` and `wiki/`, including creating a new folder. |
| [`rules/versioning.md`](rules/versioning.md) | Never change the project version without explicit user approval. |

## Git

| File | Purpose |
|---|---|
| [`git/commit-conventions.md`](git/commit-conventions.md) | Conventional Commit formatting rules for commit messages. |
| [`git/branching-strategy.md`](git/branching-strategy.md) | Branch naming, stacking order, and cross-platform change tagging. |
| [`git/pull-request-template.md`](git/pull-request-template.md) | Human-readable PR titles and the required body sections. |

## Planning

| File | Purpose |
|---|---|
| [`planning/task-workflow.md`](planning/task-workflow.md) | Intake, task decomposition, stacked branches, in-order merging. |
| [`planning/feature-implementation-steps.md`](planning/feature-implementation-steps.md) | Bottom-up implementation planning template. |

## Prompts

| File | Purpose |
|---|---|
| [`prompts/branch-and-commit.md`](prompts/branch-and-commit.md) | Standing prompt applying the branch and commit convention to every task. |
| [`prompts/folia-compatibility-check.md`](prompts/folia-compatibility-check.md) | Region-threading violation check template. |
| [`prompts/profession-generator.md`](prompts/profession-generator.md) | Few-shot profession YAML generator. |

## Creators

| File | Purpose |
|---|---|
| [`creators/instruction-creator.md`](creators/instruction-creator.md) | Creates and maintains instruction files under `.agents/`. |
| [`creators/information-creator.md`](creators/information-creator.md) | Creates and maintains documentation pages under `wiki/`. |
| [`creators/changelog-creator.md`](creators/changelog-creator.md) | Creates and maintains versioned logs under `wiki/logs/`. |
| [`creators/index-creator.md`](creators/index-creator.md) | Owns the template, split threshold, and audit for every `INDEX.md`. |

## Architecture

| File | Purpose |
|---|---|
| [`architecture/module-isolation.md`](architecture/module-isolation.md) | `api`/`common` must never import platform types. |
| [`architecture/storage-abstraction.md`](architecture/storage-abstraction.md) | Maintain the `IMCIdentityDB` facade. |

## Knowledge

| File | Purpose |
|---|---|
| [`knowledge/folia-threading-model.md`](knowledge/folia-threading-model.md) | Folia schedulers and the `PlatformScheduler` abstraction. |
| [`knowledge/identity-data-model.md`](knowledge/identity-data-model.md) | Primary vs. alternative identities, UUID derivation, caching. |

## Skills

| File | Purpose |
|---|---|
| [`skills/create-database-backend.md`](skills/create-database-backend.md) | Implement a new `IMCIdentityDB` storage backend. |
| [`skills/add-exp-action.md`](skills/add-exp-action.md) | Add a new profession EXP trigger. |
| [`skills/create-platform-implementation.md`](skills/create-platform-implementation.md) | Add a new target platform (Sponge, Fabric, etc.). |

## API

| File | Purpose |
|---|---|
| [`api/central-server-rest-specs.md`](api/central-server-rest-specs.md) | Central server REST communication contract. |

## Database

| File | Purpose |
|---|---|
| [`database/sql-injection-prevention.md`](database/sql-injection-prevention.md) | `PreparedStatement` and `StatKeys` allow-list enforcement. |
| [`database/schema-migrations.md`](database/schema-migrations.md) | Auto schema generation and idempotent re-runs. |

## Security

| File | Purpose |
|---|---|
| [`security/secret-management.md`](security/secret-management.md) | Tokens read from env/config only, never hardcoded. |
| [`security/insecure-deserialization.md`](security/insecure-deserialization.md) | Safe handling of serialized `ItemStack`/inventory blobs. |
| [`security/dependency-supply-chain.md`](security/dependency-supply-chain.md) | Dependency vulnerability scanning and shaded-jar integrity. |

## Performance

| File | Purpose |
|---|---|
| [`performance/async-first-policy.md`](performance/async-first-policy.md) | Never block the main thread on persistence. |
| [`performance/ram-caching-rules.md`](performance/ram-caching-rules.md) | RAM-first updates, async writes, bulk EXP flush. |

## Dependencies

| File | Purpose |
|---|---|
| [`dependencies/gradle-multi-project-rules.md`](dependencies/gradle-multi-project-rules.md) | `compileOnly` platform APIs; shading rules. |
| [`dependencies/java-25-toolchain.md`](dependencies/java-25-toolchain.md) | Java 25 features and the Foojay toolchain. |

## Deploy

| File | Purpose |
|---|---|
| [`deploy/github-packages-publishing.md`](deploy/github-packages-publishing.md) | Publish jars to the GitHub Maven registry. |
| [`deploy/engine-jar-export.md`](deploy/engine-jar-export.md) | Export the shaded engine jar to a test server. |

## Workflows

| File | Purpose |
|---|---|
| [`workflows/ci-pipeline.md`](workflows/ci-pipeline.md) | GitHub Actions test and shading validation. |

## Docs

| File | Purpose |
|---|---|
| [`docs/wiki-sync-rules.md`](docs/wiki-sync-rules.md) | Keep `wiki/` in sync with CLI/schema/API changes. |
| [`docs/index-maintenance.md`](docs/index-maintenance.md) | Keep every index current with structural changes. |

## Personas

| File | Purpose |
|---|---|
| [`personas/mc-plugin-architect.md`](personas/mc-plugin-architect.md) | Senior Minecraft Java developer persona. |
| [`personas/database-specialist.md`](personas/database-specialist.md) | DBA persona for dialect-agnostic, validated queries. |

## Ethics

| File | Purpose |
|---|---|
| [`ethics/safe-degradation.md`](ethics/safe-degradation.md) | Safe fallbacks on read failures; cold-boot protection. |

## Compliance

| File | Purpose |
|---|---|
| [`compliance/license-headers.md`](compliance/license-headers.md) | Retain SPDX license tags on source files. |

## Tools

| File | Purpose |
|---|---|
| [`tools/schema-validator.json`](tools/schema-validator.json) | Validate SQL dialects and MongoDB structures stay synchronized. |
| [`tools/yaml-profession-linter.json`](tools/yaml-profession-linter.json) | Lint custom profession YAML files against expected keys. |
