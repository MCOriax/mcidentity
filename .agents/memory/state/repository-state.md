---
name: repository-state
description: Current live state of the MCIdentity repository — version, stack, what is built, what is not, and where the agent-setup work currently sits.
---

# Repository State — mcidentity

Where things stand, as recorded on the date below. Facts, not rules: the rules
live in [`.agents/rules/repository.md`](../../rules/repository.md), and the
orientation page is [`.agents/wiki/context/repository-map.md`](../../wiki/context/repository-map.md).
That page answers *where things are*; this one answers *what state they are in*.
If this file and the repository disagree, the repository is right and this file
is stale.

## 2026-09-28

### Version and identity

| | |
|---|---|
| `project-version` | `4.6.1`, in [`gradle.properties`](../../../gradle.properties) |
| Latest changelog | `wiki/logs/4/6/1/CHANGELOG.md`, dated 2026-08-27 |
| Latest git tag | `v4.6.0` — there is no `v4.6.1` tag, though the 4.6.1 changelog exists |
| Group | `io.github.mcoriax` |
| Remote | `https://github.com/MCOriax/mcidentity.git`, default branch `master` |
| `LICENSE` | the bare `# License` placeholder — no license type, holder, or year |

### Stack

| | |
|---|---|
| Language | Java 25, provisioned by the `foojay-resolver` plugin in [`settings.gradle`](../../../settings.gradle) |
| Build | Gradle 9.5.0, wrapper committed; no system `gradle` is used |
| Gradle features on | configuration cache, parallel, build cache — all three, in `gradle.properties` |
| Targets | SpigotMC, PaperMC, Folia — one shaded universal jar from `platforms/engine` |
| Distribution | GitHub Packages; only `:api`, `:common`, `:bukkit` publish |
| Dependencies available | SpigotMC, PaperMC, Folia APIs, all `compileOnly` |
| Storage | SQLite (default), MySQL, PostgreSQL, MongoDB, and the central REST server |

### What is built

Seven Gradle projects plus a `platforms/` container directory. The layer order
runs one way only: `api` ← `common` ← `bukkit` ← `platforms:{spigotmc, papermc,
foliamc}` ← `platforms:engine`. Module roles and the build mechanics are in
[`wiki/information/project-structure.md`](../../../wiki/information/project-structure.md).

Tests exist in four modules and nowhere else: `common` (19), `platforms/spigotmc`
(1), `platforms:papermc` (1), `platforms/foliamc` (2). `api`, `bukkit`, and
`platforms/engine` have no test sources; a change there is verified by
compilation and by `ArchitectureBoundaryTest`.

Documentation: 19 pages under `wiki/`, and one agent-facing page at
`.agents/wiki/context/repository-map.md`.

### What is not built

* **No CI.** There is no `.github/` directory.
  [`.agents/workflows/ci-pipeline.md`](../../workflows/ci-pipeline.md) describes a
  pipeline this project expects — JDK 25, `:common:test`, shading validation —
  but nothing in the working tree runs it. Nothing has ever verified a push.
* **No tests for three of the seven projects** — see above.
* **No license.** `LICENSE` is a placeholder.
* **No `v4.6.1` tag**, against a changelog that says 4.6.1 was released
  2026-08-27.

### Agent-setup work, complete but unpushed

Six-task documentation work, tracked in
[`.agents/memory/tasks/agents-setup.md`](../tasks/agents-setup.md),
`status: done`. All six tasks are committed and the record is closed:

| Task | Branch | State |
|---|---|---|
| 1 — task record | `chore/agents-setup-plan` | committed |
| 2 — Gradle project-structure doc | `docs/gradle-project-structure` | committed |
| 3 — agent orientation page | `docs/agent-repository-map` | committed |
| 4 — repository rules refresh | `docs/gradle-module-rules` | committed |
| 5 — this state seed | `chore/memory-state-seed` | committed |
| 6 — release | `chore/agents-setup-release` | committed |

**Five commits sit on `master` that `origin/master` does not have.** The six
branches are local, unpushed, unmerged, and no pull request has been opened.
That is deliberate: pushing, opening a pull request, and merging are each gated
on an explicit yes, and none has been given. The record's `PR` column is empty
because there is nothing to number — it is not an oversight.

**No changelog entry was written for this work, and no version directory was
created.** The work is unshipped, so it belongs to whatever release carries it.
This is decision **D7** in the task record, and the rule behind it is
[`.agents/logs/changelog-policy.md`](../../logs/changelog-policy.md): a release
task records nothing in `wiki/logs/` until the work reaches the default branch,
and silence there is the correct result rather than a deferred chore.

The `INDEX.md` → `.agents/index/{scope}-index.md` migration is **not** part of
that work and has not been done. Four `INDEX.md` files remain where the current
shared set forbids them. `AGENTS.md:36-41` records this as known; the record's
decision **D2** defers it.

### Next obvious step

Push the six branches and open the pull request chain, once a human permits it.
Push first, then the record's `PR` column gets its numbers from the release
task's branch — which is the only branch that contains all six, so filling it
there rebases nothing. After the last merge, diff `master` against
`chore/agents-setup-release` and confirm the trees are identical.

### Open decisions a future session should not re-litigate

* The migration is tracked as decision **D2** in the task record: out of scope
  there, and deliberately so.
* The `403 Key limit exceeded` from the original investigation is **explained,
  and it is not a bug in this repository.** The session is routed through
  OpenRouter — `ANTHROPIC_BASE_URL` is `openrouter.ai/api` — and the error is
  that key's account-level total spend limit (`authentication_failed`, HTTP
  403). Reproduced deliberately on 2026-09-28 to confirm it still held.
* The reason it read as a session-wide failure: the main loop works while every
  subagent launch 403s, because the subagent route asks for a different model
  than the session runs. Inline work was never at risk. Retrying agents without
  raising the key's limit would fail identically every time — the fix is on
  OpenRouter's side, not in this repository.
