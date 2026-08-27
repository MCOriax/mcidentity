---
name: sonarqube-remediation
description: Template for turning a SonarCloud issue report into verified GitHub issues, stacked branches, and behaviour-preserving fixes.
---

# Prompt: SonarQube Remediation

Run this template whenever a SonarCloud report for this project is handed over as
a list of issues to fix. It converts the report into tracked GitHub issues and an
ordered set of branches before any code is touched.

This repository is hosted on GitHub only. Issues are opened with the GitHub MCP
tools or the `gh` CLI. There is no GitLab mirror and no `glab` step.

## Verify before you track

A SonarCloud report is a snapshot and goes stale. **Never open an issue for a
finding you have not re-checked against the working tree.** For each reported
finding, confirm on the current default branch that:

* the file still exists at the reported path;
* the reported line still holds the construct the rule describes — the method
  declaration for a complexity finding, the concatenation for a dynamic-SQL
  finding, the assertion-free body for a missing-test finding;
* a duplicated-literal count re-counted from source still exceeds the threshold.

Report every finding that no longer reproduces, and drop it. Report every count
that has drifted, and track the current one.

## Do not duplicate an existing issue

Before opening anything, list the repository's open **and** closed issues and
search for the rule, the file, and the literal. Extend or reopen what already
exists rather than filing a near-duplicate.

## Group by remediation kind, not by file

One issue per remediation kind, because each kind has a different review shape
and a different definition of done. The usual four:

| Kind | Definition of done |
|---|---|
| Security and error handling | The unsafe construct is gone by construction, not suppressed. |
| Missing tests | Every named test asserts the behaviour its name claims. |
| Cognitive complexity | Every listed method is at or below 15, with behaviour unchanged. |
| Duplicated literals | No listed literal is written more than once per file. |

Each issue states the file, the line, the rule, the constraint it must respect,
and the command that proves it done.

## Order the work so it stacks

Sequence the branches so that two tasks never edit the same method out of order.
Extractions come before restructurings: pull duplicated literals into constants
first, then reduce complexity, so the refactor moves code that is already clean.

## Prompt

> For each SonarCloud finding below:
>
> 1. Re-check it against the working tree and state `REAL` or `STALE` with the
>    evidence. Drop every `STALE` finding.
> 2. Name the remediation kind and the issue it belongs to.
> 3. Propose the fix in one line, then confirm it changes no observable
>    behaviour — and if it does, say exactly what changes and why that is
>    correct.
> 4. Name the constraint from `.agents/` that governs the fix: module isolation,
>    the storage facade, async-first, Folia region threading, or the SQL
>    allow-list.
> 5. Give the command that proves the finding resolved.
>
> Never satisfy a rule by suppressing it, weakening a test, or widening an
> allow-list.

Paste the SonarCloud report after this prompt.

## Conventions

This prompt does not restate the branch, commit, or pull request conventions.
They are always active and owned elsewhere:

* Intake, task splitting, branch stacking, merge order —
  [`../planning/task-workflow.md`](../planning/task-workflow.md).
* The standing branch-and-commit loop —
  [`branch-and-commit.md`](branch-and-commit.md).
* Pull request title and body —
  [`../git/pull-request-template.md`](../git/pull-request-template.md).

Reference the issue number in the pull request body, never in a commit message —
[`../git/commit-conventions.md`](../git/commit-conventions.md) forbids
issue-tracker IDs in commits.
