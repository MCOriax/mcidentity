# GUI System

[← Back to README](../../README.md)

The plugin features a robust, multi-tier GUI system for a seamless player experience.

## Main Identity GUI (1-Row)
Accessed via `/identity` without arguments. It provides:
- **Slot 0**: A button rendered as your own player head, named **"Player Identity"**. Clicking it opens the **Player Identity GUI**.
- **Slot 4**: Interactive slot to equip/unequip your **Identity Key**.

Runes and cosmetics have their own menus in their own plugins, opened with
`/rune` and `/cosmetic`; see
[MCIdentityRune](https://github.com/MCOriax/mcidentity-rune) and
[MCIdentityCosmetic](https://github.com/MCOriax/mcidentity-cosmetic).

## Player Identity GUI (Dynamic, Paginated)
Opened from Slot 0 of the Main Identity GUI. It lists every base "player"
identity you own — the always-available **Primary** identity plus every
unlocked **Alternative** identity (see [Key Features](../information/features.md)) — and
lets you switch between them:
- **Title**: `Player Identity - Page {page number}`.
- **Alternative identities**: Listed from **Slot 0** upward (index 1...n), up
  to 45 per page across the content rows. Each is shown as a head textured with
  a texture picked at random from its **own profession's available textures**
  (stable per identity, not fixed to one skin). **Clicking an alternative
  identity head equips that identity.**
- **Dynamic sizing**: The content area scales with your unlocked identities —
  9 alternatives need one content row, 10 need two, and so on. A bottom
  control row is always appended below the content.
- **Primary identity**: Shown as your own player head at the **center of the
  control row**. On any multi-page view the GUI uses the full six rows, which
  places the primary identity permanently at **Slot 49**. **Clicking it
  equips the Primary identity.**
- **Identity naming**: Each alternative identity head is named by that
  identity's **alias**, with its **profession** and **level** on the lines
  below in the lore. An identity that has not claimed an alias yet is named
  after its profession instead, so a head is never blank. The primary identity
  keeps its fixed *"Primary Identity"* name, because the base `player`
  identity always goes by your Minecraft name and never carries an alias; its
  profession and level are shown in its lore all the same.
- **No identity UUIDs**: Neither the alternative heads nor the primary head
  print an identity UUID any more. Which identity a head stands for is carried
  in the item's persistent data, not in its text, so the GUI keeps working
  whatever a head is labelled.
- **Equipped marker**: The currently equipped base identity shows *"Currently
  equipped."* in its lore; every other head shows *"Click to equip."*.
- **Back button**: The **first slot of the control row** (Slot 45 on six-row
  views) returns to the **Main Identity GUI**.
- **Pagination**: On six-row views, **Slot 48** is "Previous Page" and
  **Slot 50** is "Next Page". Each button is shown only when the
  corresponding page exists, so players with hundreds of alternative
  identities can browse them page by page.
- **Switching rules**: Switching in this GUI works between base identities
  only — a regular profession identity must be unequipped first so its key
  item is handed back. Each identity keeps its own isolated inventory and
  enderchest, which are saved and restored on every switch, and the identity
  swap cooldown applies.

## Menus owned by the add-ons

The Rune GUI and the Cosmetic GUI are **not** part of this plugin. They ship
with, and are documented by, the add-ons that own the data behind them:

- [MCIdentityRune](https://github.com/MCOriax/mcidentity-rune) — the six-row
  Rune GUI, opened with `/rune`.
- [MCIdentityCosmetic](https://github.com/MCOriax/mcidentity-cosmetic) — the
  one-row Cosmetic GUI for skins and auras, opened with `/cosmetic`.

Both add-ons read the equipped identity from this plugin's session cache, so
they are only usable while an identity is equipped.
