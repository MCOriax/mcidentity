# Commands & Permissions

[← Back to README](../README.md)

## Commands

- `/identity`: Open the main Identity GUI (1-row compact menu).
- `/identity help`: Show the help message with all available commands.
- `/identity ability get <system code>`: Admin tool to get an ability item. You can then give this to a player.
- `/identity ability create <ability_code_id> <ability_skin_id> <head_texture_base64> [custom_model_id]`: Admin tool to create an equippable ability item.
  - `ability_code_id`: The MythicMobs ability (skill) ID the item represents.
  - `ability_skin_id`: The model ID rendered for the ability. Players equipping the item store this skin alongside the ability code.
  - `head_texture_base64`: The Base64 profile-property texture applied to the player-head item.
  - `custom_model_id` (optional): A numeric `CustomModelData` value for resource-pack models. Omit it or pass `null` to keep the default model.
  - Both IDs are saved in the item's persistent data (NBT), so equipping the item through the Ability GUI records exactly which MythicMobs ability and skin to use.
- `/identity rune get <system code>`: Admin tool to get a rune item.
- `/identity get <profession|random> [texture]`: Admin tool to give a new identity key. You can provide a profession code or "random" to have one assigned upon first use. You can optionally provide a Base64 texture string.
- `/identity check`: Inspect the identity key in your hand or your currently equipped identity.
- `/identity unuse`: Unequip your current identity.
- `/identity stats`: View your remaining unspent stat points. This is a shortcut that behaves exactly like `/identity stats get`.
- `/identity stats get`: View your remaining unspent stat points.
- `/identity stats add <stat_type> <amount>`: Spend your stat points to upgrade specific attributes. Available stats are `energy`, `health`, `food`, `strength`, `defense`, `wisdom`, `luck`, `agility`, and `resilience` (e.g., `/identity stats add food 5`). Tab completion suggests every stat, including `food`.

## Permissions

- **Standard player commands** (e.g., `/identity`, `/identity stats`, `/identity unuse`): Available to **all players by default**. These do not require any permission nodes.
- `mcidentity.admin`: Required for all administrative sub-commands including `/identity ability get` and `/identity ability create`.
