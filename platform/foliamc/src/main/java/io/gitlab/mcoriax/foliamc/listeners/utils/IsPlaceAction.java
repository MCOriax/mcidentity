package io.gitlab.mcoriax.foliamc.listeners.utils;

import org.bukkit.event.inventory.InventoryAction;

public class IsPlaceAction {
    /**
     * Check if an inventory action is a place action.
     */
    public static boolean execute(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || 
               action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR;
    }
}
