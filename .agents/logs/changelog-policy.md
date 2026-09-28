---
name: changelog-policy
description: What this repository's release task records in wiki/logs/, and the one case where the correct answer is to record nothing.
---

# Changelog policy — mcidentity

The procedure that writes `wiki/logs/` is the shared changelog creator, at
`{shared}/creators/changelog-creator.md`. Read it there. This file does not
restate it and does not replace it — it is a repository-local answer to one
question that file leaves open.

**The question.** A release task is supposed to write a changelog entry. What
does it write when the work it is releasing has not shipped?

## Why this needed an answer here

The shared creator settles both sides of the question and leaves nothing in the
middle:

* a released version's log is never edited, so an entry cannot go into
  `wiki/logs/4/6/1/CHANGELOG.md`, which is headed `**Released:** 2026-08-27`;
* a new version directory is a version claim and needs explicit approval, so the
  entry cannot simply move to `wiki/logs/4/6/2/`.

A plan whose release task runs before anything is pushed therefore has nowhere
to write, and the two obvious answers are both wrong. Adding the entry to the
released log states that documents authored today shipped in a release cut
weeks ago. Creating a version directory claims a version that is not being cut.
This repository hit that case on 2026-09-28 and the plan's own decision **D3**
sent the entry to the released 4.6.1 log, which was wrong.

## The rule

**A release task records nothing in `wiki/logs/` when the work has not shipped.**
Silence there is correct, not a gap, and it is not a deferred chore.

The test is not whether the version directory exists, and not whether a
changelog could technically be written. It is whether the work has reached the
default branch:

| State | Where the entry goes |
|---|---|
| Branches local, nothing pushed | Nowhere. Record the omission in the task record and stop. |
| Pushed, pull request open, unmerged | Nowhere. Same — the entry rides the merge, not the push. |
| Merged to the default branch | The version log for the release that carries it, once that version exists. |

**Never add an entry to a log headed with a release date that has passed.**
A released log is a statement about a shipped build, and this repository ships
from `master`.

**A plan may not pre-empt this by directing its release task at a released
log.** If a plan's decisions name a changelog path, check the path is not
already released before following it. A decision that does not survive its own
check is superseded and the supersession is recorded in the task record — see
decision **D7** in
[`.agents/memory/tasks/agents-setup.md`](../memory/tasks/agents-setup.md).

## What the release task does instead

* Records in the task record that no entry was written, and why, in the same
  commit that closes the record.
* Creates no version directory. Claiming no version is the point.
* Leaves the `PR` column empty while nothing is pushed, per decision **D5** in
  the task record — placeholders are not acceptable values.
* Carries the entry into the release that eventually ships the work, which is a
  decision for whoever cuts that release.

## What this file does not settle

* Whether a version directory may be created — that is
  `{shared}/rules/versioning.md`, and it needs an explicit yes.
* The changelog's section order or entry format —
  `{shared}/creators/changelog-creator.md`.
* What goes in a log for work that *has* shipped. Nothing here restricts that,
  and nothing here should be read as permission to log work that has not.
