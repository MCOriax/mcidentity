---
name: agents-index
description: Sole authority indexing and managing the .agents/ instruction set for the MCIdentity repository.
---

# Agents Index

This file is the primary directory structure and index for all agent
instructions used alongside the MCIdentity repository. It is the sole authority
for the `.agents/` instruction set.

Agents MUST read the root `AGENTS.md`, then the root `INDEX.md`, then this file
before acting. The root `INDEX.md` must NEVER be used to dictate or write files
inside `.agents/`; management of `.agents/` is delegated exclusively here.

## Rules

| File | Purpose |
|---|---|
| [`rules/directories.md`](rules/directories.md) | Agent directory architecture and strict isolation rules for the `.agents/` tree. |
| [`rules/repository.md`](rules/repository.md) | Rules specific to this repository (module boundaries, storage facade, async). |

## Skills

| File | Purpose |
|---|---|
| [`skills/create_database_backend.md`](skills/create_database_backend.md) | Implement a new `IMCIdentityDB` storage backend. |
| [`skills/add_exp_action.md`](skills/add_exp_action.md) | Add a new profession EXP trigger. |
| [`skills/create_platform_implementation.md`](skills/create_platform_implementation.md) | Add a new target platform (Sponge, Fabric, etc.). |

## Tools

| File | Purpose |
|---|---|
| [`tools/schema_validator.json`](tools/schema_validator.json) | Validate SQL dialects and MongoDB structures stay synchronized. |
| [`tools/yaml_profession_linter.json`](tools/yaml_profession_linter.json) | Lint custom profession YAML files against expected keys. |

## Knowledge

| File | Purpose |
|---|---|
| [`knowledge/folia_threading_model.md`](knowledge/folia_threading_model.md) | Folia schedulers and the `PlatformScheduler` abstraction. |
| [`knowledge/identity_data_model.md`](knowledge/identity_data_model.md) | Primary vs. alternative identities, UUID derivation, caching. |

## Personas

| File | Purpose |
|---|---|
| [`personas/mc_plugin_architect.md`](personas/mc_plugin_architect.md) | Senior Minecraft Java developer persona. |
| [`personas/database_specialist.md`](personas/database_specialist.md) | DBA persona for dialect-agnostic, validated queries. |

## Ethics

| File | Purpose |
|---|---|
| [`ethics/safe_degradation.md`](ethics/safe_degradation.md) | Safe fallbacks on read failures; cold-boot protection. |

## Git

| File | Purpose |
|---|---|
| [`git/commit_conventions.md`](git/commit_conventions.md) | Conventional commit formatting rules. |
| [`git/branching_strategy.md`](git/branching_strategy.md) | Branch off `main`; tag cross-platform vs. API changes. |

## Dependencies

| File | Purpose |
|---|---|
| [`dependencies/gradle_multi_project_rules.md`](dependencies/gradle_multi_project_rules.md) | `compileOnly` platform APIs; shading rules. |
| [`dependencies/java_25_toolchain.md`](dependencies/java_25_toolchain.md) | Java 25 features and Foojay toolchain. |

## Planning

| File | Purpose |
|---|---|
| [`planning/feature_implementation_steps.md`](planning/feature_implementation_steps.md) | Bottom-up implementation planning template. |

## Architecture

| File | Purpose |
|---|---|
| [`architecture/module_isolation.md`](architecture/module_isolation.md) | `api`/`common` must never import platform types. |
| [`architecture/storage_abstraction.md`](architecture/storage_abstraction.md) | Maintain the `IMCIdentityDB` facade. |

## API

| File | Purpose |
|---|---|
| [`api/central_server_rest_specs.md`](api/central_server_rest_specs.md) | Central server REST communication contract. |

## Database

| File | Purpose |
|---|---|
| [`database/sql_injection_prevention.md`](database/sql_injection_prevention.md) | `PreparedStatement` and `StatKeys` allow-list enforcement. |
| [`database/schema_migrations.md`](database/schema_migrations.md) | Auto schema generation and idempotent re-runs. |

## Security

| File | Purpose |
|---|---|
| [`security/secret_management.md`](security/secret_management.md) | Tokens read from env/config only, never hardcoded. |
| [`security/insecure_deserialization.md`](security/insecure_deserialization.md) | Safe handling of serialized `ItemStack`/inventory blobs. |
| [`security/dependency_supply_chain.md`](security/dependency_supply_chain.md) | Dependency vulnerability scanning and shaded-jar integrity. |

## Performance

| File | Purpose |
|---|---|
| [`performance/async_first_policy.md`](performance/async_first_policy.md) | Never block the main thread on persistence. |
| [`performance/ram_caching_rules.md`](performance/ram_caching_rules.md) | RAM-first updates, async writes, bulk EXP flush. |

## Compliance

| File | Purpose |
|---|---|
| [`compliance/license_headers.md`](compliance/license_headers.md) | Retain Apache-2.0 / SPDX license tags. |

## Deploy

| File | Purpose |
|---|---|
| [`deploy/github_packages_publishing.md`](deploy/github_packages_publishing.md) | Publish jars to the GitHub Maven registry. |
| [`deploy/engine_jar_export.md`](deploy/engine_jar_export.md) | Export the shaded engine jar to a test server. |

## Workflows

| File | Purpose |
|---|---|
| [`workflows/ci_pipeline.md`](workflows/ci_pipeline.md) | GitHub Actions test and shading validation. |

## Prompts

| File | Purpose |
|---|---|
| [`prompts/folia_compatibility_check.md`](prompts/folia_compatibility_check.md) | Region-threading violation check template. |
| [`prompts/profession_generator.md`](prompts/profession_generator.md) | Few-shot profession YAML generator. |

## Docs

| File | Purpose |
|---|---|
| [`docs/wiki_sync_rules.md`](docs/wiki_sync_rules.md) | Keep `wiki/` in sync with CLI/schema/API changes. |
| [`docs/index_maintenance.md`](docs/index_maintenance.md) | Keep `INDEX.md` current with root structure changes. |
