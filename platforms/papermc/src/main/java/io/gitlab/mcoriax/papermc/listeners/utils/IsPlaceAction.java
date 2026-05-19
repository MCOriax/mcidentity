package io.gitlab.mcoriax.papermc.listeners.utils;

import org.bukkit.event.inventory.InventoryAction;

public class IsPlaceAction {
    /**
     * Check if an inventory action is a place action.
     * @param action The inventory action to check.
     * @return true if it is a place action, false otherwise.
     */
    public static boolean execute(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || 
               action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR;
    }
}
