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

This index lists **local** instructions only. Branching, commits, pull requests,
the task workflow, the creators, and the placement and versioning rules are served
by the `lxagents-agents-base` connector and are addressed as
`agents://{folder}/{file}.md`; route into them through
`agents://index/root-index.md`. They are deliberately absent below — a local copy
would override the shared file by `name` and then silently go stale.

## Rules

| File | Purpose |
|---|---|
| [`rules/repository.md`](rules/repository.md) | Rules specific to this repository: module boundaries, storage facade, build commands. |

## Planning

| File | Purpose |
|---|---|
| [`planning/feature-implementation-steps.md`](planning/feature-implementation-steps.md) | Bottom-up implementation planning template. |

## Prompts

| File | Purpose |
|---|---|
| [`prompts/folia-compatibility-check.md`](prompts/folia-compatibility-check.md) | Region-threading violation check template. |
| [`prompts/profession-generator.md`](prompts/profession-generator.md) | Few-shot profession YAML generator. |
| [`prompts/sonarqube-remediation.md`](prompts/sonarqube-remediation.md) | Verify a SonarCloud report, track it as GitHub issues, and fix it in order. |

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

## Wiki

Agent-facing knowledge. Distinct from the human `wiki/`, indexed by
`wiki/INDEX.md`; the two never duplicate each other.

| File | Purpose |
|---|---|
| [`wiki/context/repository-map.md`](wiki/context/repository-map.md) | Orientation before any work: layout, build and test commands, entry points, generated paths, build traps. |

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

## Memory

| File | Purpose |
|---|---|
| [`memory/tasks/agents-setup.md`](memory/tasks/agents-setup.md) | Record of the six-task agent setup: build doc, orientation page, rules refresh, memory seed. |
| [`memory/tasks/shared-instruction-adoption.md`](memory/tasks/shared-instruction-adoption.md) | Record of the duplicate-instruction audit and what it removed. |

## Tools

| File | Purpose |
|---|---|
| [`tools/schema-validator.json`](tools/schema-validator.json) | Validate SQL dialects and MongoDB structures stay synchronized. |
| [`tools/yaml-profession-linter.json`](tools/yaml-profession-linter.json) | Lint custom profession YAML files against expected keys. |
