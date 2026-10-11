---
name: release-workflow
description: Why the core publishes and tags itself from publish.yml when an untagged project-version reaches master.
---

# Decision: automatic publish and tag

mcidentity (core) has `.github/workflows/publish.yml`, display name Publish (added as `release.yml` in PR #122, renamed in PR #123, both merged 2026-10-11 at owen's request; old runs stay under the name Release). On every push to `master` it reads `project-version`, checks the remote for tag `v{version}`, and stops if it exists. If missing it runs `./gradlew build`, `./gradlew publish` (api, common, bukkit to GitHub Packages, using `GITHUB_ACTOR` and `GITHUB_TOKEN`), then pushes the annotated tag. First run published 4.7.0 and tagged `v4.7.0` (tag sits on the workflow PR's merge commit, not the 4.7.0 release merge).

**Why:** owen asked for the tag-then-publish flow to be automatic; a second publish of the same version is rejected by GitHub Packages, so tagging comes last.

**How to apply:** a release is now just the approved version bump merged to master. Never merge a version bump casually; it publishes immediately. If publish worked but tagging failed, tag by hand instead of re-running. Add-on repos have no Actions CI (only Sonar automatic analysis) and read the core package with a `read:packages` token.
