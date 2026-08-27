package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class ProcessEquip {
    /**
     * Process the equipment of an identity.
     * @param plugin The plugin instance.
     * @param player The player equipping the identity.
     * @param identityUuid The identity UUID.
     * @param itemToEquip The item stack being equipped.
     * @param base64 The serialized item string.
     * @param reopenGui Whether to reopen the GUI.
     */
    public static void execute(JavaPlugin plugin, Player player, UUID identityUuid, ItemStack itemToEquip, String base64, boolean reopenGui) {
        Schedulers.forEntity(player).run(plugin, st1 -> {
            if (player.isDead()) {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                return;
            }

            MCIdentityEquipEvent equipEvent = new MCIdentityEquipEvent(player, identityUuid, itemToEquip);
            Bukkit.getPluginManager().callEvent(equipEvent);

            if (equipEvent.isCancelled()) {
                RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "Equipping was cancelled.");
                return;
            }

            // Swapping away from a base identity — the primary or an
            // alternative of any profession: hand its storage back (saved and
            // cleared by the unequip listeners) before the new identity's
            // inventory loads.
            Optional<ActiveIdentitySession> current = MCIdentityManager.getSession(player.getUniqueId());
            final boolean fromBase = current.isPresent()
                && MCIdentityManager.isWearingBaseIdentity(player.getUniqueId());
            if (fromBase && !releaseBaseIdentity(plugin, player, current.get(), itemToEquip)) {
                return;
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64)
                .thenAccept(v -> onEquipped(plugin, player, reopenGui))
                .exceptionally(ex -> {
                    onEquipFailed(plugin, player, itemToEquip, ex, fromBase);
                    return null;
                });
        }, null);
    }

    /**
     * Announces that the base identity is being swapped away from, so its
     * storage is handed back before the new inventory loads.
     * @param plugin The plugin instance.
     * @param player The player equipping.
     * @param current The base identity session being left.
     * @param itemToEquip The item being equipped, refunded when a listener refuses.
     * @return true when the equip may continue.
     */
    private static boolean releaseBaseIdentity(JavaPlugin plugin, Player player,
                                               ActiveIdentitySession current, ItemStack itemToEquip) {
        MCIdentityUnequipEvent baseUnequip = new MCIdentityUnequipEvent(player, current.identityUuid());
        Bukkit.getPluginManager().callEvent(baseUnequip);
        if (baseUnequip.isCancelled()) {
            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "Equipping was cancelled.");
            return false;
        }
        return true;
    }

    /**
     * Clears the pending flag, re-titles the item, and refreshes the visuals.
     * @param plugin The plugin instance.
     * @param player The player equipping.
     * @param reopenGui Whether the GUI reopens afterwards.
     */
    private static void onEquipped(JavaPlugin plugin, Player player, boolean reopenGui) {
        MCIdentityManager.setEquipPending(player.getUniqueId(), false);
        MCIdentityManager.setSwapCooldown(player.getUniqueId());
        // The session now holds this identity, so its item is re-titled
        // from the live alias, profession, and level. This is what
        // brings an item made before alias naming existed up to date,
        // rather than leaving it on its original name forever. The GUI
        // below renders the stored item, so it waits for the rewrite
        // instead of reopening on the old name.
        RefreshIdentityItem.execute(plugin, player).thenRun(() ->
            Schedulers.forEntity(player).run(plugin, st2 -> {
                if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                    MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> statsListener.updateVisuals(player, session));
                }

                if (reopenGui) {
                    new MCIdentityCommand(plugin).openIdentityGui(player);
                }
                player.sendMessage(ChatColor.GREEN + "Identity equipped successfully!");
            }, null));
    }

    /**
     * Reports the failure and gives the player back whichever identity they
     * were left without.
     * @param plugin The plugin instance.
     * @param player The player equipping.
     * @param itemToEquip The item that was being equipped.
     * @param ex Why the equip failed.
     * @param fromBase Whether the base identity's storage was already handed off.
     */
    private static void onEquipFailed(JavaPlugin plugin, Player player, ItemStack itemToEquip,
                                      Throwable ex, boolean fromBase) {
        MCIdentityManager.setEquipPending(player.getUniqueId(), false);
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
        String failureMessage = ChatColor.RED + MessageComponents.value(
                "mcidentity.gui.error.equip_failed.prefix",
                "Failed to equip identity: ",
                errorMessage,
                "mcidentity.gui.error.equip_failed.suffix",
                ""
        );
        if (fromBase) {
            // The base storage was already handed off, so re-equip the
            // base profession to restore it along with the item.
            EquipBaseProfession.execute(plugin, player, itemToEquip, failureMessage, false);
        } else {
            RefundItemSafely.execute(plugin, player, itemToEquip, failureMessage);
        }
    }
}
