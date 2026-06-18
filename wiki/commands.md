# Commands & Permissions

[← Back to README](../README.md)

## Commands

- `/identity`: Open the main Identity GUI (1-row compact menu).
- `/identity help`: Show the help message with all available commands.
- `/identity ability get <system code>`: Admin tool to get an ability item. You can then give this to a player.
- `/identity rune get <system code>`: Admin tool to get a rune item.
- `/identity get <profession|random> [texture]`: Admin tool to give a new identity key. You can provide a profession code or "random" to have one assigned upon first use. You can optionally provide a Base64 texture string.
- `/identity check`: Inspect the identity key in your hand or your currently equipped identity.
- `/identity unuse`: Unequip your current identity.
- `/identity stats get`: View your remaining unspent stat points.
- `/identity stats add <stat_type> <amount>`: Spend your stat points to upgrade specific attributes (e.g., strength, defense).

## Permissions

- `mcidentity.admin`: Required for all administrative sub-commands including `/identity ability get`.
