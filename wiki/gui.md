# GUI System

[← Back to README](../README.md)

The plugin features a robust, multi-tier GUI system for a seamless player experience.

## Main Identity GUI (1-Row)
Accessed via `/identity` without arguments. It provides:
- **Slot 2**: Opens the **Ability GUI**.
- **Slot 4**: Interactive slot to equip/unequip your **Identity Key**.
- **Slot 6**: Opens the **Rune GUI**.
- **Slot 8**: Opens the **Cosmetic GUI**.

## Cosmetic GUI (1-Row)
Provides a menu to manage visual aspects of the identity:
- **Slot 4**: Displays a **Skin** placeholder.
- **Slot 7**: Displays an **Aura** placeholder.
- **Security**: This menu is only accessible if the player has an active identity equipped.

## Ability & Rune GUIs (6-Rows)
These GUIs are dynamically bound to the database:
- **Fetching**: Upon opening, the plugin fetches the currently equipped abilities/runes for the active identity.
- **Placeholders**: Empty slots are shown as **Red Stained Glass Panes**.
- **Equipped Items**: Equipped abilities/runes are shown as **Lime Stained Glass Panes** with their respective display names.
- **Security**: These menus are only accessible if the player has an active identity equipped.
