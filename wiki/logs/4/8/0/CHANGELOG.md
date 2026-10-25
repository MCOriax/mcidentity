# Changelog — 4.8.0

**Released:** 2026-10-25

Builds against the Minecraft 26.3 server APIs, lets a player ask an MCAgents AI about
their own identity through two read-only tools, and moves releasing onto the shared
`MCEngine/actions` workflows, which now publish only after a `release/{version}` pull
request merges. Server owners: nothing to do; the MCAgents tools appear only when
MCAgents is installed. Maintainers: a version bump merged from any branch other than
`release/{version}` no longer publishes.

## Added

- `tools` module (`io.github.mcoriax.mcidentity.tools`) with two read-only, player-only
  MCAgents tools, `mcidentity_get_my_identity` and `mcidentity_get_my_stats`. Both read
  the RAM session cache only and refuse any caller that is not an online player. The
  module is bundled into the universal jar by `platforms:engine` and is not published.
- `MCAgentsToolRegistrar` and `BukkitIdentityToolPort` (`bukkit`, package
  `bridge.mcagents`) register the tools with MCAgents by reflection at the end of
  `onEnable`; without MCAgents the plugin logs one info line and carries on.
- `softdepend: [MCAgents]` in all four `plugin.yml` files.
- `wiki/reference/agent-tools.md` — what the tools return and who can use them.

## Changed

- The spigotmc, papermc and foliamc platforms compile against the Minecraft 26.3 APIs
  (spigot `26.3-R0.1-SNAPSHOT`, paper `26.3.build`, folia `26.3.build.5-alpha`, as
  Folia has no stable 26.3 build yet). `bukkit` now declares `org.jetbrains:annotations`
  as `compileOnly`, because spigot-api 26.3 no longer supplies it.
- `.github/workflows/publish.yml` and `release.yml` are thin callers of the shared
  workflows in `MCEngine/actions`; the Release caller grants `packages: read`.
- Publish runs only when a pull request from a `release/{version}` branch merges to
  `master`, and fails when `{version}` is not the merged `project-version`; any other
  push or merge publishes nothing, and `workflow_dispatch` on `master` retries. Release
  follows Publish runs on `release/**` and `master`.
- The Publish workflow that builds, publishes and tags automatically (#122, renamed to
  `publish.yml` in #123) shipped with tag `v4.7.0` but is missing from the 4.7.0 log,
  which was written before it merged; it is recorded here instead.
