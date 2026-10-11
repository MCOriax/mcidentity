---
name: ci-pipeline
description: Rules for the GitHub Actions test and shading-validation pipeline.
---

# Workflows: CI Pipeline

Rules for the GitHub Actions pipeline.

## Jobs

1. **Toolchain.** Provision JDK 25 (Foojay). Do not fall back to an older JDK.
2. **Test.** Run the `:common:test` suite. It must exercise the provider and
   manager, base profession helpers, the `StatKeys` allow-list, the backend
   factory, the real SQLite backend, the MySQL/PostgreSQL/MongoDB backends over
   mocked drivers, and the central server client against a stub HTTP server.
3. **Shading.** Validate the `platforms:engine` shadow task — the shaded jar
   builds, bundles the three platform modules, and carries only its own
   `plugin.yml` (no duplicates).

## Publish workflow

`.github/workflows/publish.yml` is the only workflow that exists today. It runs
the full `./gradlew build` and, when `v{project-version}` is not yet a tag,
publishes the library modules and tags the commit. It is specified in
`deploy/github-packages-publishing.md`; the jobs above remain the pipeline this
project expects for pushes and pull requests.

## Rules

* Fail the pipeline on any test failure or shading error.
* Cache Gradle dependencies keyed on the version catalog for speed.
* Publishing steps read `GITHUB_ACTOR`/`GITHUB_TOKEN` from secrets, never
  hardcoded (`security/secret-management.md`).
