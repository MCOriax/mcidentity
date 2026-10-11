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
| `project-version` | `4.7.0`, in [`gradle.properties`](../../../gradle.properties) |
| Latest changelog | `wiki/logs/4/7/0/CHANGELOG.md`, dated 2026-10-11 |
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

* **No test CI.** The only workflow is `.github/workflows/release.yml`, which
  builds, publishes and tags when an untagged version reaches `master`.
  [`.agents/workflows/ci-pipeline.md`](../../workflows/ci-pipeline.md) describes a
  pipeline this project expects — JDK 25, `:common:test`, shading validation —
  but nothing runs it on a push or pull request.
* **No tests for three of the seven projects** — see above.
* **No license.** `LICENSE` is a placeholder.
* **No `v4.6.1` tag**, against a changelog that says 4.6.1 was released
  2026-08-27.

### Agent-setup work, released and in review

Six-task documentation work, tracked in
[`.agents/memory/tasks/agents-setup.md`](../tasks/agents-setup.md),
`status: done`. All six tasks are committed, all six branches are on the
remote, and the record is closed:

| Task | Branch | PR |
|---|---|---|
| 1 — task record | `chore/agents-setup-plan` | #114 |
| 2 — Gradle project-structure doc | `docs/gradle-project-structure` | #115 |
| 3 — agent orientation page | `docs/agent-repository-map` | #116 |
| 4 — repository rules refresh | `docs/gradle-module-rules` | #117 |
| 5 — this state seed | `chore/memory-state-seed` | #118 |
| 6 — release + changelog policy | `chore/agents-setup-release` | #119 |

**The six pull requests are stacked and must merge in order.** #114 targets
`master`; each later one targets the *branch* of the one before it, not
`master`. #114's body carries the whole chain as its index. The push, the pull
requests, and the merge were each put to the user separately and each answered
explicitly — that is why the gates are recorded as held rather than assumed.

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

Merge the chain in order, #114 through #119, re-targeting each stacked pull
request to its new base *before* merging it rather than after — a forge only
auto-retargets when the base branch is deleted on merge, and where that setting
is off the merge succeeds while the default branch silently stays behind. After
the last merge, diff `master` against `chore/agents-setup-release` and confirm
the trees are identical. "Merged" is a claim about a pull request, not about
the default branch.

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
