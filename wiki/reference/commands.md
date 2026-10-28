# Commands & Permissions

[← Back to README](../../README.md)

## Commands

- `/identity`: Open the main Identity GUI (1-row compact menu).
- `/identity help`: Show the help message with all available commands.
- `/identity wiki`: Send clickable links to the project wiki, one per line (`https://mcoriax.github.io` and `https://mcoriax.gitlab.io`); clicking a line opens it in your browser.
- `/identity get <profession|random> [texture]`: Admin tool to give a new identity key. You can provide a profession code or "random" to have one assigned upon first use. You can optionally provide a Base64 texture string.
- `/identity check`: Inspect the identity key in your hand or your currently equipped identity.
- `/identity unequip`: Unequip your current identity.
- `/identity stats`: View your remaining unspent stat points. This is a shortcut that behaves exactly like `/identity stats get`.
- `/identity stats get`: View your remaining unspent stat points.
- `/identity stats add <stat_type> <amount>`: Spend your stat points to upgrade specific attributes. Available stats are `energy`, `health`, `food`, `strength`, `defense`, `wisdom`, `luck`, `agility`, and `resilience` (e.g., `/identity stats add food 5`). Tab completion suggests every stat, including `food`.
- `/identity alias`: Show the alias of the identity you are currently wearing, or a hint on how to claim one when it has none. On the base `player` identity it reports that the identity always goes by your Minecraft name.
- `/identity alias <name>`: Claim `<name>` as the alias of the identity you are currently wearing. The name may contain **letters and digits only** (`a-z`, `A-Z`, `0-9`) and may be at most **32 characters** long; anything else is rejected. An alias is **unique across every identity on the server**, and that check **ignores case** — if someone already holds `Alex`, then `alex` and `ALEX` are refused too. Your alias is stored exactly as you typed it, so `Alex` stays `Alex`. A claimed alias immediately replaces your Minecraft name in chat and in the tab list for as long as that identity is equipped.
  - **An alias is permanent.** Once an identity carries one it is never renamed: running the command again is refused with *"This identity alias is already set as ..."*. Claim the name you want to keep.
  - **The base `player` identity has no alias.** It always goes by your Minecraft name, so the command is refused there. Every other identity, alternative base identities included, can claim one.
- `/identity base add <player> <amount>`: Admin tool to grant a player additional **alternative base identities**, assigning each new slot a **random non-player profession**. For example, `/identity base add MCOriax 2` adds 2 alternative identity slots to MCOriax, each a random class.
- `/identity base add <player> <profession> <amount>`: The same grant, but every new slot is assigned the specific `<profession>` you name (e.g., `/identity base add MCOriax warrior 3`). The `player` profession **cannot** be added as a sub-identity: it is reserved for the primary identity, so the command is rejected and the sender is notified if it is requested. The slot count is stored in the database and the new identities are registered immediately under deterministic UUIDs, so they appear in the [Player Identity GUI](../guides/gui.md) right away. Requires `mcoriax.mcidentity.command.base` (operators by default); the sub-command and its tab completion are hidden from anyone without it.

## Permissions

Every sub-command checks its own node. Player nodes default to `true`, so a server that
never touches permissions behaves as before; administrative nodes default to operators.
`/help`, tab completion and the command itself all read the same node, so a player is
neither shown nor allowed a sub-command they lack.

| Command | Node | Default |
|---|---|---|
| `/identity` | `mcoriax.mcidentity.command.use` | `true` |
| `/identity help` | `mcoriax.mcidentity.command.help` | `true` |
| `/identity wiki` | `mcoriax.mcidentity.command.wiki` | `true` |
| `/identity check` | `mcoriax.mcidentity.command.check` | `true` |
| `/identity unequip` | `mcoriax.mcidentity.command.unequip` | `true` |
| `/identity stats [get\|add]` | `mcoriax.mcidentity.command.stats` | `true` |
| `/identity alias [name]` | `mcoriax.mcidentity.command.alias` | `true` |
| `/identity get` | `mcoriax.mcidentity.command.get` | `op` |
| `/identity base add` | `mcoriax.mcidentity.command.base` | `op` |

Parent nodes:

- `mcoriax.mcidentity.*` (`op`): every node above, plus `mcoriax.mcidentity.admin`.
- `mcoriax.mcidentity.admin` (`op`): `command.get` and `command.base`.
- `mcidentity.admin` (`op`): the node used before the `mcoriax` prefix. It is still honoured
  and grants `mcoriax.mcidentity.admin`, so an existing grant keeps working.

Revoking a player node (for example `mcoriax.mcidentity.command.alias: false`) removes that
sub-command for the group it is revoked from.
