# GUI, Items, and Serialization

## 1. NBT Data & PersistentDataContainer
*   Use `NamespacedKey` and `PersistentDataContainer` to store custom data on items.
*   Supported data types: `PersistentDataType.STRING`, `PersistentDataType.BYTE`, etc.
*   *Verification*: Always check if an item has meta (`item.hasItemMeta()`) before accessing the container.

## 2. Custom Player Heads (SkullMeta)
*   To create custom textured heads, DO NOT use player names.
*   Use `PlayerProfile` and Base64 texture strings.
*   *Implementation*:
```java
    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
    profile.setProperty(new ProfileProperty("textures", base64TextureString));
    skullMeta.setPlayerProfile(profile);
```

## 3. Item Serialization
*   Use the internal `ItemStackSerializer` utility to convert items to Base64 strings for database storage.
*   `ItemStackSerializer.serialize(item)`
*   `ItemStackSerializer.deserialize(base64)`

## 4. GUI Management
*   GUIs are standard `Inventory` objects interacting via `InventoryClickEvent`.
*   Ensure all GUI clicks explicitly use `event.setCancelled(true)` to prevent players from stealing UI components.
*   Placeholder slots should be filled with Stained Glass Panes (`BLACK_STAINED_GLASS_PANE`, `RED_STAINED_GLASS_PANE`, etc.) with empty components (`Component.empty()`).
