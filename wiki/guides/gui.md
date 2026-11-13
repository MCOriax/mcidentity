# GUI System

[← Back to README](../../README.md)

## Main Identity GUI (1-Row)

Opened with `/identity` without arguments.

- **Slot 0**: A book named **"Professions"**. Clicking it opens the profession list.
- **Slot 4**: The profession you wear, shown as its head, with its level and EXP and your
  unspent stat points in the lore. Display only.

Runes and cosmetics have their own menus in their own plugins, opened with `/rune` and
`/cosmetic`; see [MCIdentityRune](https://github.com/MCOriax/mcidentity-rune) and
[MCIdentityCosmetic](https://github.com/MCOriax/mcidentity-cosmetic).

## Profession list (6-Row)

Opened from slot 0 of the main GUI. Title: `Professions`.

- Every registered profession is listed from slot 0, the base `player` profession
  first and the rest by code, up to 45 professions.
- Each entry is a head using the profession's first configured skin, or your own head
  when the profession has no skin.
- The lore marks each entry:
  - *Currently worn*: clicking it takes the profession off (`/identity unwear`),
    except for the base `player` profession.
  - *Click to wear*: clicking it wears the profession (`/identity wear`).
  - *Locked*: you lack `mcoriax.mcidentity.profession.*` and
    `mcoriax.mcidentity.profession.<code>`.
- **Slot 49** is the back button to the main GUI.

A click follows the same rules as the commands: the `wear` or `unwear` command node,
the profession node, the swap cooldown, and the cancellable profession events. The
inventory and enderchest are never touched.
