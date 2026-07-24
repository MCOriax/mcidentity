---
name: github-packages-publishing
description: Procedure for publishing the api, common, and bukkit jars to the GitHub Maven registry.
---

# Deploy: GitHub Packages Publishing

Publish the `api`, `common`, and `bukkit` jars to the GitHub Maven registry.

## Authentication

* Authenticate with `GITHUB_ACTOR` (username) and `GITHUB_TOKEN` (password) read
  from the environment. Never hardcode credentials. See
  `security/secret_management.md`.

## Procedure

1. Ensure the version follows Semantic Versioning (`Major.Minor.Patch`) and has
   been bumped for this release.
2. Run the Gradle publish task for the `api`, `common`, and `bukkit` modules
   targeting the GitHub Packages Maven repository.
3. Confirm the artifacts appear in the registry with the expected coordinates.

## Notes

* Only publish the library modules (`api`, `common`, `bukkit`) — the shaded
  engine jar is exported separately (`deploy/engine_jar_export.md`).
* Platform APIs are `compileOnly` and must not be bundled into published jars.
