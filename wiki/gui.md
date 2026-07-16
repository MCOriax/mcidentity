# GUI System

[← Back to README](../README.md)

The plugin features a robust, multi-tier GUI system for a seamless player experience.

## Main Identity GUI (1-Row)
Accessed via `/identity` without arguments. It provides:
- **Slot 0**: A button rendered as your own player head, named **"Player Identity"**. Clicking it opens the **Player Identity GUI**.
- **Slot 2**: Opens the **Ability GUI**.
- **Slot 4**: Interactive slot to equip/unequip your **Identity Key**.
- **Slot 6**: Opens the **Rune GUI**.
- **Slot 8**: Opens the **Cosmetic GUI**.

## Player Identity GUI (Dynamic, Paginated)
Opened from Slot 0 of the Main Identity GUI. It lists every base "player"
identity you own — the always-available **Primary** identity plus every
unlocked **Alternative** identity (see [Key Features](features.md)) — and
lets you switch between them:
- **Title**: `Player Identity - Page {page number}`.
- **Alternative identities**: Listed from **Slot 0** upward (index 1...n), up
  to 45 per page across the content rows. Each is shown as a head textured with
  its **own profession's head texture**. **Clicking an alternative identity
  head equips that identity.**
- **Dynamic sizing**: The content area scales with your unlocked identities —
  9 alternatives need one content row, 10 need two, and so on. A bottom
  control row is always appended below the content.
- **Primary identity**: Shown as your own player head at the **center of the
  control row**. On any multi-page view the GUI uses the full six rows, which
  places the primary identity permanently at **Slot 49**. **Clicking it
  equips the Primary identity.**
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

## Cosmetic GUI (1-Row)
Provides a menu to manage visual aspects of the identity:
- **Slot 5**: Displays a **Skin** placeholder.
- **Slot 7**: Displays an **Aura** placeholder.
- **Security**: This menu is only accessible if the player has an active identity equipped.

## Ability & Rune GUIs (6-Rows)
These GUIs are dynamically bound to the database:
- **Fetching**: Upon opening, the plugin fetches the currently equipped abilities/runes for the active identity.
- **Placeholders**: Empty slots are shown as **Red Stained Glass Panes**.
- **Equipped Items**: Equipped abilities/runes are shown as **Lime Stained Glass Panes** with their respective display names.
- **Security**: These menus are only accessible if the player has an active identity equipped.
