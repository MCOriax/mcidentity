---
name: security-dependency-supply-chain
description: Vulnerability scanning and integrity rules for Gradle dependencies and the shaded engine jar.
---

# Security: Dependency & Supply Chain Security

The build pulls third-party libraries through Gradle and bundles several of them
into the shaded `platforms:engine` jar. A poisoned or vulnerable dependency ships
straight to every server that installs the plugin, so dependency hygiene is a
security control, not just maintenance.

## Rules

* Pin exact dependency versions through the Gradle version catalog. Never use
  dynamic versions (`+`, `latest.release`) that could pull a substituted build.
* Scan for known vulnerabilities in CI (e.g. OWASP dependency-check, Dependabot,
  or Snyk) and fail the pipeline on new high-severity advisories. Wire this into
  `workflows/ci_pipeline.md`.
* Review and patch flagged dependencies promptly; prefer a patched minor/patch
  version over silently suppressing an advisory.
* Only bundle what is needed. Server platform APIs stay `compileOnly` and are
  never shaded (`dependencies/gradle_multi_project_rules.md`); the engine jar
  carries only its own `plugin.yml`.
* Keep shaded relocations consistent so a bundled library cannot be shadowed by a
  malicious same-named class from another plugin on the server.
* Resolve dependencies from trusted registries only. Do not add an unofficial or
  unpinned repository, and keep any private-registry credentials in the
  environment (`security/secret_management.md`).

## Verification

* A dependency or version bump must keep `:common:test` green and the
  `platforms:engine` shadow task producing a single valid jar
  (`workflows/ci_pipeline.md`).
