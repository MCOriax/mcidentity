---
name: versioning-rules
description: Never change the project version without explicit user approval; how to propose a bump instead.
---

# Rules: Versioning

## The rule

**Never change the project version on your own initiative.** Always ask the user
first and wait for an explicit answer before editing any version carrier.

## What this covers

Every version carrier that exists in this repository, and every one that could be
added later:

| Carrier | Where |
|---|---|
| `project-version` | `gradle.properties` |
| Gradle wrapper / plugin versions | `gradle/`, `build.gradle`, `settings.gradle` |
| `plugin.yml` version fields | `platforms/*/src/main/resources/plugin.yml` |
| Git tags and release drafts | GitHub releases |
| Log directories | `wiki/logs/{Major}/{Minor}/{Patch}/` |

Creating a new `wiki/logs/{Major}/{Minor}/{Patch}/` directory **is a version
claim**, so it needs the same approval as editing `gradle.properties`. See
[`../creators/changelog-creator.md`](../creators/changelog-creator.md).

## How to propose a bump

When a change looks like it warrants a bump, do not apply it. Propose it:

1. State the current version, read from `gradle.properties`.
2. State the proposed version.
3. State which of major / minor / patch it is, and why.
4. List every file that would change.

Then stop and wait for an explicit answer.

## Never

* Never re-tag an existing release.
* Never rewrite an existing released version's log directory. Corrections go in
  the next version's log.
* Never create a version directory "just to have somewhere to write" — ask which
  version applies first.
