---
name: gradle-plugin-adoption
description: Task record for moving mcidentity's root build.gradle onto the shared io.github.mcengine.build-logic convention plugin, released as 4.11.1.
---

# Gradle plugin adoption

**Status:** done

## Goal

Replace the build logic this repository's root `build.gradle` copies from the other
plugins with `id 'io.github.mcengine.build-logic' version '0.1.0'` from
MCEngine/gradle-plugin, without changing what the build produces.

## Objective

* `settings.gradle` resolves the plugin from
  `https://maven.pkg.github.com/MCEngine/gradle-plugin`.
* The root `build.gradle` is the `plugins { }` block plus an `mcengineBuild { }` block
  for what differs from the plugin's defaults.
* No module names a Shadow version; the plugin supplies Shadow 9.4.1.
* Published coordinates stay `io.github.mcoriax:mcidentity-{api,common,bukkit}`.
* `./gradlew build` passes; Publish and Release green on 4.11.1.

## Decisions

The owner was away and asked for the recommended option at every fork.

* Release as **4.11.1**.
* Instruction files under `.agents/rules/` that describe the old root build are left for
  the owner as discovery findings.

## Tasks

| # | Title | Branch | PR |
|---|---|---|---|
| 1 | Plan and task record | `plan/gradle-plugin-adoption` | #141 |
| 2 | Apply the convention plugin | `build/gradle-plugin` | #142 |
| 3 | Release 4.11.1 | `release/4.11.1` | #143 |

## Progress

### Task 1 — plan/gradle-plugin-adoption

Wrote this record.

### Task 2 — build/gradle-plugin

`settings.gradle` gained the `pluginManagement` repository for the plugin; the root
`build.gradle` is now the plugin plus `mcengineBuild { }`; module builds dropped the
Shadow version. Updated `wiki/information/project-structure.md` and `.agents/wiki/context/repository-map.md`, which quoted the old root build. Verified locally with `./gradlew build` against the plugin
published to Maven Local.

### Task 3 — release/4.11.1

`project-version` → 4.11.1, `wiki/logs/4/11/1/CHANGELOG.md`, the log index row
where the repository keeps one, and the PR column. Record closed.
