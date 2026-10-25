# MCAgents Tools

[← Back to README](../../README.md)

When the [MCAgents](https://github.com/MCPaimon/mcagents) plugin is installed,
MCIdentity registers two read-only tools with it, so a player chatting with an
AI in game can ask about their own identity.

## The tools

| Tool | What it returns |
|---|---|
| `mcidentity_get_my_identity` | The caller's active identity: profession (display name and code), alias, whether it is a base identity, level and level cap, EXP towards the next level against the EXP one level costs, and unspent stat points. |
| `mcidentity_get_my_stats` | The stats of the caller's active identity — Health, Energy, Strength, Defense, Wisdom, Luck, Agility, Resilience, Food — its live health, energy, and food, and its unspent stat points. |

Neither tool takes arguments. Both belong to the MCAgents categories
`identity` and `player_info`.

## Who can use them

Any player. The tools need no permission node and no operator status, because
they only ever read the caller's own data:

* The caller must be an in-game player who is online on this server. Any other
  caller — the console, the server account, a guild, an unknown account —
  gets `Error: This tool is only available to an in-game player.`
* The player read is always the caller. No argument can point a tool at
  another player.
* Nothing is written. There are no mutating tools.

A player with no identity equipped is told so. If the data cannot be read, the
tool answers with an error message rather than failing the AI's turn.

## How the data is read

Both tools read the RAM session cache that MCIdentity keeps for every online
player, so a tool call never reaches the database and never blocks a server
thread. On Folia the player is only looked up by UUID; no entity state is
touched off its owning thread.

## MCAgents is optional

MCAgents is a soft dependency (`softdepend: [MCAgents]` in `plugin.yml`), so it
enables first when present. MCIdentity has no compile dependency on it: the
tools are registered by reflection through MCAgents' `getProvider()` and
`registerTool(AITool)`.

* **MCAgents not installed or disabled** — one info line in the log; nothing
  else changes.
* **MCAgents present but incompatible** — one warning in the log; MCIdentity
  carries on without the tools.

The tool code lives in the `tools` module
(`io.github.mcoriax.mcidentity.tools`), and the registration in
`bukkit/bridge/mcagents`.
