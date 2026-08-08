# Changelog — 4.6.0

**Released:** 2026-08-08

Adds the agent instruction architecture and the structured documentation tree to
the repository. No plugin behavior changed in this release.

## Added

- `.agents/creators/` with four creator instructions: `instruction-creator.md`,
  `information-creator.md`, `changelog-creator.md`, and `index-creator.md`. Each
  embeds the branch and commit convention and the discovery protocol in full.
- `.agents/planning/task-workflow.md` — Goal/Objective/Detail intake, task
  decomposition, one stacked branch per task, in-order execution, and one pull
  request per branch merged in order after user approval.
- `.agents/git/pull-request-template.md` — human-readable pull request titles and
  the required Overview / Added / Modified / Deleted / Summary body.
- `.agents/rules/versioning.md` — forbids changing `project-version` or creating a
  log directory without explicit user approval.
- `.agents/prompts/branch-and-commit.md` — standing prompt so the branch and
  commit convention never has to be restated.
- `wiki/INDEX.md` and `wiki/logs/INDEX.md` routing indexes.
- `wiki/information/overview.md`, carrying the project detail moved out of
  `README.md`.
- `LICENSE` placeholder, pending the licensing decision.

## Changed

- Every file under `.agents/` renamed from `snake_case` to kebab-case, with all
  cross-links repaired.
- `.agents/rules/directories.md` rewritten as the placement authority for both
  `.agents/` and `wiki/`, including the algorithm for creating a new folder when
  no existing one fits.
- `.agents/INDEX.md` rewritten to the canonical index template with a scope line
  and a parent link.
- Root `INDEX.md` reduced to a pure router that lists indexes only; the module and
  package layout it used to carry moved to `wiki/information/architecture.md`.
- Root `AGENTS.md` rewritten as an overview with the mandatory reading order,
  routing protocol, iron rules, and discovery protocol.
- `README.md` reduced to an overview; its detail moved into `wiki/` pages.
- `wiki/` flattened pages reorganized into `information/`, `environments/`,
  `reference/`, and `guides/`.
