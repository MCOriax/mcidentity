package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import org.bukkit.event.inventory.InventoryAction;

public class IsTakeAction {
    /**
     * Check if an inventory action is a take action.
     * @param action The inventory action to check.
     * @return true if it is a take action, false otherwise.
     */
    public static boolean execute(InventoryAction action) {
        return action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF || 
               action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME || 
               action == InventoryAction.MOVE_TO_OTHER_INVENTORY;
    }
}
