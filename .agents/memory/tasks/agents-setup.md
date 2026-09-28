---
name: agents-setup
description: Record of the six-task agent setup — the Gradle project-structure doc, the agent orientation page, the repository rules refresh, and the memory state seed.
status: done
---

# Task: Agents Setup

## Goal

Give this repository a documented build, a documented orientation page, and a
seeded memory, without touching a line of Java and without bumping the version.

Six tasks, one branch each, stacked. Task 1 is this record; task 6 is the
release. Nothing is pushed and no pull request is opened by this work — the
branches are local, so the `PR` column below is deliberately empty.

## Mode

**Consumer.** The shared instruction set is resolved through the
`lxagents-agents-base` connector and is never vendored into this repository.
The repository contributes only what is its own: build rules, module boundaries,
and orientation.

## Tasks

| # | Title | Branch | Files / areas | PR |
|---|---|---|---|---|
| 1 | Task record | `chore/agents-setup-plan` | `.agents/memory/tasks/agents-setup.md`, `.agents/INDEX.md` | #114 |
| 2 | Gradle project-structure doc (human) | `docs/gradle-project-structure` | `wiki/information/project-structure.md`, `wiki/information/architecture.md`, `wiki/INDEX.md` | #115 |
| 3 | Agent orientation page | `docs/agent-repository-map` | `.agents/wiki/context/repository-map.md`, `.agents/INDEX.md`, `AGENTS.md` | #116 |
| 4 | Repository rules refresh | `docs/gradle-module-rules` | `.agents/rules/repository.md`, `.agents/dependencies/gradle-multi-project-rules.md` | #117 |
| 5 | Memory state seed | `chore/memory-state-seed` | `.agents/memory/state/repository-state.md` | #118 |
| 6 | Release | `chore/agents-setup-release` | `tasks/agents-setup.md`, `.agents/memory/state/repository-state.md`, `.agents/logs/changelog-policy.md`, `.agents/INDEX.md` | #119 |

**All six pull requests are open, stacked, and merge in this order.** #114
targets `master`; #115 targets `chore/agents-setup-plan`; #116 targets
`docs/gradle-project-structure`; #117 targets `docs/agent-repository-map`;
#118 targets `docs/gradle-module-rules`; #119 targets `chore/memory-state-seed`.
#114's body carries the whole chain as its index. The column was filled here,
on the release branch, because that is the only branch containing all six —
writing it on branch 1 would have left every later pull request behind and
forced a rebase of the stack.

## Decisions

**D1 — The build is documented once, for humans, in `wiki/`.** The Gradle
structure, the layer order, the dependency-scoping table, the publishing gate,
the universal jar, and the configuration-cache constraints have no home today.
`wiki/information/architecture.md` has a `## 2. Modules` section but treats
`platforms` as one block and never explains the build. Task 2 adds a dedicated
page and cross-links it; the agent-facing page in task 3 points at it rather
than restating it.

**D2 — The `INDEX.md` migration is out of scope.** The current shared set forbids
the filename `INDEX.md` anywhere and wants indexes at
`.agents/index/{scope}-index.md`. This repository still has four of them
(`INDEX.md`, `.agents/INDEX.md`, `wiki/INDEX.md`, `wiki/logs/INDEX.md`), and
`AGENTS.md:36-41` already admits it. Migrating them is a multi-file link refactor
and a separate piece of work. This record does not attempt it, and every index
this task touches is updated in its existing convention.

**D3 — No new version directory.** The project stays at `4.6.1`, and
`wiki/logs/4/6/0/` is not created. Anything versioned goes into the existing
`wiki/logs/4/6/1/CHANGELOG.md`. Creating a version directory *is* a version
claim, and there is no release to claim.

**D4 — `LICENSE` stays the `# License` placeholder.** Inventing a license type, a
holder, or a year is a legal statement, and there are none to infer.

**D5 — No pull requests, *superseded 2026-09-28*.** Originally: the gates stand,
ask before opening a pull request, ask before merging, the branches stay local,
and the `PR` column stays empty rather than holding placeholder numbers. The
gates were honoured — each was put to the user and each was answered
explicitly. The user then authorized the push, the pull request chain, and the
merge in full, so the second half of this decision no longer holds. What
survives is the rule about placeholders: the column is filled with real numbers
once they exist, never with stand-ins.

**D6 — Documentation only.** No Java source, no `build.gradle`, and nothing under
`.gradle/` or `build/` is touched. The build is read, never run, by this work.

**D7 — D3 is superseded on the changelog, and no changelog entry is written.**
D3 sent versioned material to `wiki/logs/4/6/1/CHANGELOG.md`. That file is
headed `**Released:** 2026-08-27`, and the changelog creator's rule is that a
released log is never edited. Writing this work into it would assert that
documents authored on 2026-09-28 shipped in a release cut a month earlier — and
they have not shipped at all, since no branch has been pushed. D3's real
intent, *claim no new version*, is honoured by making no version claim. The
entry belongs to whatever release actually carries this work, and that decision
is not this task's to make. The question D3 left open is now answered in
[`.agents/logs/changelog-policy.md`](../../logs/changelog-policy.md), which sets
out when a release task records nothing and why silence in `wiki/logs/` is the
correct result rather than a deferred chore.

## Entries

### Task 1 — Task record (2026-09-28)

**Branch:** `chore/agents-setup-plan` · **PR:** #114

Wrote this record, and registered it in `.agents/INDEX.md` in the same commit.
The investigation behind the decisions above read `settings.gradle`, the root
`build.gradle`, `gradle.properties`, all seven subproject build files, and the
`ArchitectureBoundaryTest` source; nothing was written outside `.agents/`.

**Open at this point:** every task from 2 to 6, plus the `INDEX.md` migration
(D2), which this work explicitly does not attempt.

### Task 2 — Gradle project-structure doc (2026-09-28)

**Branch:** `docs/gradle-project-structure` · **PR:** #115

Added `wiki/information/project-structure.md`, the document that did not exist:
seven projects and the skipped `platforms/` container, the one-direction layer
order, the scoping table with the reason each row differs, the publishing gate
and the `artifactId` collision it prevents, the universal jar with its
`plugin.yml` dedup rule and explicit `dependsOn` wiring, the
`ArchitectureBoundaryTest` task inputs, and the configuration-cache
constraints. Cross-linked it from `wiki/information/architecture.md` — both in
the introduction and under `### platforms`, which previously described the
container as if it were a single module — and added the row to `wiki/INDEX.md`,
all in one commit.

**One correction to the investigation.** The claim was that adding a platform
module means extending the Gradle `fileTree`. The `fileTree` glob
(`platforms/*/src/main/java/**/*.java`) already covers any new module, so the
task re-runs on its own. The hardcoded list is the test's own
`PLATFORM_SOURCE_ROOTS` array in `ArchitectureBoundaryTest`, and a module
missing from it is silently *unchecked*, not silently unchecked-and-passing.
The document says so.

**Also corrected:** "only engine compiles against the Folia API" is true of the
*union* requirement, but `platforms:foliamc` necessarily compiles against it
too. The real constraint is that engine is the only module needing every
platform type in one compilation unit, and the only one for which declaring all
three APIs would be a mutually exclusive Gradle capability conflict.

**Open at this point:** tasks 3 to 6, plus D2.

### Task 3 — Agent orientation page (2026-09-28)

**Branch:** `docs/agent-repository-map` · **PR:** #116

Added `.agents/wiki/context/repository-map.md` and registered it in
`.agents/INDEX.md` under a new `## Wiki` section, in the same commit. The page
carries what lives where, the build/test/run/export/publish commands with what
each actually does, the five entry points and the role of each, the generated
paths to leave alone, and the build traps.

The traps are the part worth keeping. Each one is a way this build fails
*quietly* rather than loudly: a module with no `build.gradle` is skipped without
a warning; a new platform module makes `ArchitectureBoundaryTest` go green
while quietly not reading the new sources, because the Gradle `fileTree` glob
picks them up but the test's own `PLATFORM_SOURCE_ROOTS` array does not; and a
build script that breaks configuration-cache compatibility does not fail, it
just makes every build slower.

**The false note in `AGENTS.md` is gone.** It claimed `.agents/wiki/` did not
exist in this repository and pointed orientation at `wiki/information/` instead.
It now points at the new page, and the row above it in the same table already
referenced a path that did not exist — that row now resolves.

**Found while reading:** there is no `.github/` directory. The
`workflows/ci-pipeline.md` file describes a pipeline this project expects but
nothing in the working tree runs it, so the orientation page says so rather
than letting an agent cite it as evidence a check passed.

**Open at this point:** tasks 4 to 6, plus D2.

### Task 4 — Repository rules refresh (2026-09-28)

**Branch:** `docs/gradle-module-rules` · **PR:** #117

Rewrote `.agents/dependencies/gradle-multi-project-rules.md` around the
constraints a change must not break, and added a routing table for the scoping
question. The new sections: a module with no `build.gradle` is skipped
silently; the `compileOnly` rule covers the *shared* modules, not just platform
APIs; `compileOnly` does not reach the test classpath, which is why each
platform module re-adds `testImplementation project(':bukkit')`; the publishing
gate and why the `artifactId` qualification is load-bearing; and the
`ArchitectureBoundaryTest` inputs, with the note that a new platform module
leaves the test green while it stops reading the new sources.

`rules/repository.md` gained the verified command table (`exportJar`, and the
fact that root `clean` touches only the root `build/`), the test-coverage map,
and the two quiet boundary-enforcement traps. Its module-boundary bullet links
`module-isolation.md` for the rule rather than restating it, and adds only the
build facts that file does not carry.

**One false claim corrected.** The old shading section said to "keep relocations
consistent so shaded dependencies never clash with other plugins on the server".
There is no `relocate` call anywhere in the build — the shadow is a plain
`runtimeClasspath` merge, and every bundled class keeps its original
coordinates. That is correct today, because everything bundled is part of this
plugin, but the sentence described a protection that does not exist. It now
says so, and says what would have to change if a shared library were ever
shaded.

Both index rows updated in the same commit. No shared rule was restated; the
fresh material is repository facts, and the shared conventions stay addressed
through `{shared}`.

**Open at this point:** tasks 5 and 6, plus D2.

### Task 5 — Memory state seed (2026-09-28)

**Branch:** `chore/memory-state-seed` · **PR:** #118

Added `.agents/memory/state/repository-state.md` and registered it in
`.agents/INDEX.md` in the same commit, under `## Memory`, ahead of the two task
records. It is written to be overwritten in place — a state file describes the
present, so a later session replaces the body rather than appending a second
dated section, and the file says so.

It carries the version and identity table, the stack, what is built, what is
not, the branch-by-branch state of this work, and the next obvious step. It
points at `repository-map.md` for orientation and at the rules file for rules
rather than restating either, and it opens by saying that the repository wins
where the two disagree.

**Four things surfaced while writing it, none of which any existing document
recorded.**

* There is no `v4.6.1` tag. The newest tag is `v4.6.0`, against a
  `wiki/logs/4/6/1/CHANGELOG.md` dated 2026-08-27. A previous release's changelog
  shipped without its tag; nothing in the repository claims this is intentional.
* Nothing in this repository has ever run CI. There is no `.github/`, so the
  4.6.1 release claims "two assertion-free tests" and a SonarCloud sweep with
  nothing in the tree that would have caught a regression.
* `origin/master` is four commits behind `master`; this stack is local only.
* The `403 Key limit exceeded` failure recorded in the original investigation is
  **explained**, and the explanation is not a bug. This repository's session is
  routed through OpenRouter — `ANTHROPIC_BASE_URL` points at `openrouter.ai/api`
  — and the error is that key's account-level total spend limit, reported as
  `authentication_failed` with HTTP 403. It reproduces on demand, and it was
  reproduced deliberately rather than assumed still true.
* The reason it looked like a session-wide failure is worth keeping. The main
  loop kept working throughout while every subagent launch failed instantly,
  because the subagent route requests a *different* model than the session
  runs. So the failure looked total but was scoped to one route, and inline
  work was never actually at risk. Retrying agents before the key's limit
  changes would have failed identically every time.

**A conflict worth naming.** The shared set registers memory in
`.agents/index/memory-index.md`, which does not exist here. D2 puts the
`INDEX.md` migration out of scope, and this repository's own `AGENTS.md` and
`INDEX.md` make `.agents/INDEX.md` the sole authority over `.agents/`. So the
row went into `.agents/INDEX.md`. The shared rule is not overridden — nothing
declares an override — it simply has nowhere to land until D2 is done, and
creating `memory-index.md` early would have meant creating one piece of the
migration D2 defers.

**Open at this point:** task 6, plus D2.

### Task 6 — Release (2026-09-28)

**Branch:** `chore/agents-setup-release` · **PR:** #119

Closed the record. `status: done`, task 6's own entry, D7 recorded, and
`.agents/memory/state/repository-state.md` brought forward so it does not
contradict the repository the moment this commit lands.

**Verification, run before the commit and passed.** Every relative link in every
tracked markdown file resolves — no broken targets. The index tree is complete:
`.agents/INDEX.md` carries every `.agents/` file, `wiki/INDEX.md` carries every
`wiki/` page, and the two changelogs are owned by `wiki/logs/INDEX.md`, which
`wiki/INDEX.md` links. A scan for session links across tracked markdown came
back empty. Nothing under `build/`, `.gradle/`, or any source tree was touched,
so there was nothing for the Gradle build to verify — D6 has the build read, not
run, throughout.

**D3 turned out to be wrong and was not followed.** It sent the release entry to
`wiki/logs/4/6/1/CHANGELOG.md`, a log headed `**Released:** 2026-08-27`. The
changelog creator's rule is that a released log is never edited, and this work
is not merely newer than that release — it is unpushed. Writing it there would
have put a dated-2026-09-28 change into a log claiming a 2026-08-27 release.
D7 records the supersession. **No changelog was written, and no version
directory was created**, so this work claims no version. The entry goes into
whichever release carries it.

**`PR` column left empty on purpose.** Nothing is pushed, so there is nothing to
number, and D5 forbids placeholders.

**What is left for a human.** All six branches are local. Nothing has been
pushed, no pull request exists, and `origin/master` is five commits behind. The
push, the pull request, and the merge are all still gated and were not touched.

**Still open, and not this work's to close:** D2, the `INDEX.md` →
`.agents/index/{scope}-index.md` migration. Four `INDEX.md` files remain, and
the shared set forbids that filename anywhere.

### Task 6, redone — Changelog policy (2026-09-28)

**Branch:** `chore/agents-setup-release` · **PR:** #119

Task 6 re-run at the user's request. The outcome is unchanged — still no
changelog entry, still no version claimed — but the judgement behind it is now
a written rule instead of one call made once.

Added [`.agents/logs/changelog-policy.md`](../../logs/changelog-policy.md) and
registered a new `## Logs` section in `.agents/INDEX.md`, in the same commit. The
file answers the one question the shared changelog creator leaves open: what does
a release task record when the work it is releasing has not shipped. It gives the
test — has the work reached the default branch — and the three-way table, and it
names the failure that prompted it, which is D3 sending an entry to a log headed
`Released: 2026-08-27`.

**Deliberately not created.** `mcidentity/.agents/creators/changelog-creator.md`.
That file existed until commit `a49ebee`, which deleted it as one of the eleven
copies that shadowed the shared set, and the 4.6.1 changelog records the removal.
Recreating it would reverse a release decision and put shared content back in
this repository. The new file shadows no shared `name` and copies no shared
body; it links to `{shared}/creators/changelog-creator.md` for the procedure
rather than restating it.

**One half of a rule could not be carried out.** The placement authority says a
new folder must be registered in its own tables *and* in the index owning the
scope, in the same commit. The index half is done. The tables half lives in
`{shared}/rules/directory-architecture.md`, and a consuming repository does not
edit the shared set — it proposes. So `logs/` is not in that file's §B folder
table, and is reported as a finding rather than applied.

**Verification re-run after the change:** every relative link in every tracked
markdown file still resolves, including the four new ones from
`changelog-policy.md` into this record and into `.agents/`, and the new `## Logs`
row resolves. `.agents/logs/changelog-policy.md` is the only file added to
`.agents/`, and it appears in exactly one index.

**Closed.** Nothing else in this work is outstanding except D2, which is
deferred by decision rather than by omission.
