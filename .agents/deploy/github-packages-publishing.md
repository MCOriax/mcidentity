---
name: github-packages-publishing
description: Procedure for publishing the api, common, and bukkit jars to the GitHub Maven registry.
---

# Deploy: GitHub Packages Publishing

Publish the `api`, `common`, and `bukkit` jars to the GitHub Maven registry.

## Authentication

* Authenticate with `GITHUB_ACTOR` (username) and `GITHUB_TOKEN` (password) read
  from the environment. Never hardcode credentials. See
  `security/secret-management.md`.

## Procedure

A release is published by `.github/workflows/publish.yml`, not by hand. The
version is the approved one in `gradle.properties`; merging its
`release/{version}` pull request to `master` is the trigger.

1. Ensure the version follows Semantic Versioning (`Major.Minor.Patch`) and has
   been bumped and approved for this release.
2. Merge the release branch (`release/{version}`) to `master`. Publish runs only
   when a pull request from a `release/{version}` branch merges; any other merge
   or push publishes nothing. It fails when `{version}` in the branch name is not
   the merged `project-version`, then looks for the `v{version}` tag on the
   remote.
3. **Tag exists:** the run stops.
   **Tag missing:** the workflow runs `./gradlew build`, then
   `./gradlew publish` for the `api`, `common`, and `bukkit` modules, and only
   then creates the annotated tag `v{version}` on the merge commit.
4. Confirm the artifacts appear in the registry with the expected coordinates.

The workflow tags after publishing, so a failed publish leaves no tag and the
next run retries. A run can also be started by hand (`workflow_dispatch`) from `master`
to retry. If publishing succeeded but tagging failed, GitHub Packages rejects a
second publish of the same version: create the `v{version}` tag by hand on the
published commit instead of re-running.

## Manual fallback

Only when the workflow cannot run: export `GITHUB_ACTOR` and `GITHUB_TOKEN`
(`write:packages`) and run `./gradlew publish`, then create and push the
`v{version}` tag yourself.

## Notes

* Only publish the library modules (`api`, `common`, `bukkit`) — the shaded
  engine jar is exported separately (`deploy/engine-jar-export.md`).
* Platform APIs are `compileOnly` and must not be bundled into published jars.
