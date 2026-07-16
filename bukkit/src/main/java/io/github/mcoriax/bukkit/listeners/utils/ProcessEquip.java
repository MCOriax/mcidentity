package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.ActionType;
import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.MessageComponents;
import io.github.mcoriax.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
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
            if (fromBase) {
                MCIdentityUnequipEvent baseUnequip = new MCIdentityUnequipEvent(player, current.get().identityUuid());
                Bukkit.getPluginManager().callEvent(baseUnequip);
                if (baseUnequip.isCancelled()) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "Equipping was cancelled.");
                    return;
                }
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                MCIdentityManager.setSwapCooldown(player.getUniqueId());
                Schedulers.forEntity(player).run(plugin, st2 -> {
                    if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                        MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> statsListener.updateVisuals(player, session));
                    }

                    if (reopenGui) {
                        new MCIdentityCommand(plugin).openIdentityGui(player);
                    }
                    player.sendMessage(ChatColor.GREEN + "Identity equipped successfully!");
                }, null);
            }).exceptionally(ex -> {
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
                return null;
            });
        }, null);
    }
}
