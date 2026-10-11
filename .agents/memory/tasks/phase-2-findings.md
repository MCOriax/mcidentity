---
name: phase-2-findings
description: Phase 2 size and duplication measurements across the MCOriax repos, and what was consolidated into the core.
---

# Phase 2 findings: size and duplication

Measured with jscpd (exact token-level clones, Java and JS only, tests, docs and generated files excluded) on the latest main of every repo.

## Inside each repo: already lean

| Repo | Duplicated lines | Share |
|---|---|---|
| mcidentity | 181 | 0.93% |
| mcidentity-skill | 84 | 1.24% |
| mcidentity-rune | 38 | 0.92% |
| mcidentity-cosmetic | 26 | 0.74% |
| server-expressjs | 0 | 0% |

What remains inside a repo is structural similarity (for example `FinalizeUnboundIdentity` and `InitializeIdentity` share a shape but differ in logic, and the two skill events differ only by name and Javadoc). Merging those would add indirection for little gain, so I left them.

## Across the plugin repos: the real duplication (about 2,260 duplicated lines)

Pairs of repos copying each other:

| Pair | Lines |
|---|---|
| cosmetic and rune | 429 |
| core and rune | 418 |
| core and cosmetic | 417 |
| core and skill | 353 |
| rune and skill | 329 |
| cosmetic and skill | 317 |

Biggest copied files, by duplicated lines:

| What | Lines | Where |
|---|---|---|
| `MCIdentityBridge` | 257 | rune, skill, cosmetic |
| `Schedulers`, `BukkitPlatformScheduler`, `FoliaPlatformScheduler` | 432 | all four plugins |
| `MongoDB` | 172 | all four plugins |
| `Abstract…Plugin` | 142 | rune and cosmetic |
| central HTTP client (`send`, `write`, `read`, health check, auth) | about 90 per client | core, rune, skill, cosmetic |
| `Messages`, `ItemStackSerializer` | 170 | across plugins |
| engine plugin classes | 210 | across plugins |

Also: 15 `.agents/` instruction files are byte-identical across rune and cosmetic (and 2 across rune, skill and cosmetic, and `index-maintenance.md` in three). Per the shared-set rules those belong in the shared instruction set, which I can only report, not write.

## Why I stopped before touching the Java repos

Removing the cross-repo copies needs one place for the shared code, and that is a structural decision with release consequences, not a refactor I should slip into a PR. The choice was put to the owner.

## Done so far

server-expressjs PR 44: one table template instead of two copies of the schema (262 to 192 lines, output byte-identical, 64 tests pass).

## Outcome (2026-10-11)

The owner chose to keep shared plugin code in this core repository rather than a new one,
published to GitHub Packages by the publish workflow (see
[`../decisions/release-workflow.md`](../decisions/release-workflow.md)).

| Repo | PRs merged | What |
|---|---|---|
| server-expressjs | #44 | One table template for both schemas |
| mcidentity | #120, #121, #122, #123 | `CentralHttpClient`, core 4.7.0, the publish workflow (published 4.7.0, tagged `v4.7.0`), workflow renamed to `publish.yml` |
| mcidentity-rune, -skill, -cosmetic | rune #15, skill #20, cosmetic #18 | Central clients built on the core's `CentralHttpClient` |
| mcidentity-rune, -skill, -cosmetic | rune #16, skill #21, cosmetic #19 | Shared scheduler types from the core; minimum core 4.2.0 |

Still deferred: `MCIdentityBridge`, `MongoDB`, sharing the Folia scheduler, the abstract
plugin base, and the engine plugin classes. Add-on version bumps still need the owner's
approval.
