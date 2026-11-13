# Commands & Permissions

[← Back to README](../../README.md)

## Commands

- `/identity`: Open the main Identity GUI. Slot 0 opens the profession list; slot 4 shows the worn profession, its level and EXP, and your unspent stat points.
- `/identity help`: Show the commands you may use.
- `/identity wiki`: Send clickable links to the project wiki (`https://mcoriax.github.io` and `https://mcoriax.gitlab.io`).
- `/identity check`: Show the worn profession, its level and EXP, your stat points, and all nine stats.
- `/identity wear <profession>`: Wear a profession. Tab completion lists only the professions you may wear. Refused while the swap cooldown runs, while another change is in progress, or when a listener cancels `MCProfessionEquipEvent`.
- `/identity unwear`: Take off the worn profession and return to the base `player` profession. Fires the cancellable `MCProfessionUnequipEvent`.
- `/identity stats` or `/identity stats get`: View your unspent stat points.
- `/identity stats add <stat_type> <amount>`: Spend stat points on `energy`, `health`, `food`, `strength`, `defense`, `wisdom`, `luck`, `agility`, or `resilience` (for example `/identity stats add food 5`).

## Permissions

Every sub-command checks its own node, and every node defaults to `true`. `/help`, tab
completion and the command itself all read the same node, so a player is neither shown nor
allowed a sub-command they lack.

| Command | Node | Default |
|---|---|---|
| `/identity` | `mcoriax.mcidentity.command.use` | `true` |
| `/identity help` | `mcoriax.mcidentity.command.help` | `true` |
| `/identity wiki` | `mcoriax.mcidentity.command.wiki` | `true` |
| `/identity check` | `mcoriax.mcidentity.command.check` | `true` |
| `/identity stats [get\|add]` | `mcoriax.mcidentity.command.stats` | `true` |
| `/identity wear` | `mcoriax.mcidentity.command.wear` | `true` |
| `/identity unwear` | `mcoriax.mcidentity.command.unwear` | `true` |

### Wearing a profession

| Node | Grants | Default |
|---|---|---|
| `mcoriax.mcidentity.profession.*` | Wearing every profession | `true` |
| `mcoriax.mcidentity.profession.<code>` | Wearing one profession, such as `mcoriax.mcidentity.profession.miner` | not declared |

The base `player` profession needs no node. To limit a group to some professions, set
`mcoriax.mcidentity.profession.*: false` for that group and grant the single nodes.

### Parent node

- `mcoriax.mcidentity.*` (`op`): every command node, every AI tool node, and
  `mcoriax.mcidentity.profession.*`.
