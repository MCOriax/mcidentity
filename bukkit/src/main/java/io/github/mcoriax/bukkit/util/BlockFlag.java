package io.github.mcoriax.bukkit.util;

import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * Helper for marking blocks with a boolean metadata flag and checking it later.
 *
 * <p>EXP listeners use this to remember that a block location was already rewarded
 * for a one-time action (stripping a log, carving a pumpkin, ...), preventing the
 * place / modify / break / replace EXP farm at a fixed location. The flag lives in
 * Bukkit's in-memory metadata store, so it is reset on server restart, which is an
 * acceptable trade-off for an anti-exploit guard.</p>
 */
public final class BlockFlag {

    private BlockFlag() {
        // Utility class; not meant to be instantiated.
    }

    /**
     * Checks whether a block already carries the given flag from this plugin.
     * @param block The block to inspect.
     * @param plugin The owning plugin.
     * @param key The metadata key.
     * @return true if the flag is set by this plugin.
     */
    public static boolean isSet(Block block, Plugin plugin, String key) {
        if (!block.hasMetadata(key)) {
            return false;
        }
        for (MetadataValue value : block.getMetadata(key)) {
            if (value.getOwningPlugin() == plugin && value.asBoolean()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the given flag on a block.
     * @param block The block to mark.
     * @param plugin The owning plugin.
     * @param key The metadata key.
     */
    public static void set(Block block, Plugin plugin, String key) {
        block.setMetadata(key, new FixedMetadataValue(plugin, Boolean.TRUE));
    }
}
