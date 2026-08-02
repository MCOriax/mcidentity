# Commands & Permissions

[← Back to README](../README.md)

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
- `/identity base add <player> <profession> <amount>`: The same grant, but every new slot is assigned the specific `<profession>` you name (e.g., `/identity base add MCOriax warrior 3`). The `player` profession **cannot** be added as a sub-identity: it is reserved for the primary identity, so the command is rejected and the sender is notified if it is requested. The slot count is stored in the database and the new identities are registered immediately under deterministic UUIDs, so they appear in the [Player Identity GUI](gui.md) right away. Requires OP (the `mcidentity.admin` permission); the sub-command and its tab completion are hidden from regular players.

## Permissions

- **Standard player commands** (e.g., `/identity`, `/identity stats`, `/identity unequip`): Available to **all players by default**. These do not require any permission nodes.
- `mcidentity.admin`: Required for all administrative sub-commands including `/identity get` and `/identity base`.
