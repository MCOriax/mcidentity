package io.gitlab.mcoriax.papermc.listeners.utils;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.bukkit.event.MCIdentityEquipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.papermc.listeners.MCIdentityStatsListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
    public static void execute(MCIdentityPlugin plugin, Player player, UUID identityUuid, ItemStack itemToEquip, String base64, boolean reopenGui) {
        player.getScheduler().run(plugin, st1 -> {
            if (player.isDead()) {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                return;
            }
            
            MCIdentityEquipEvent equipEvent = new MCIdentityEquipEvent(player, identityUuid, itemToEquip);
            Bukkit.getPluginManager().callEvent(equipEvent);
            
            if (equipEvent.isCancelled()) {
                RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.equip_cancelled", "Equipping was cancelled.").color(NamedTextColor.RED));
                return;
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                MCIdentityManager.setSwapCooldown(player.getUniqueId());
                player.getScheduler().run(plugin, st2 -> {
                    if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                        MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> statsListener.updateVisuals(player, session));
                    }

                    if (reopenGui) {
                        new MCIdentityCommand(plugin).openIdentityGui(player);
                    }
                    player.sendMessage(Component.translatable("mcidentity.status.equipped_success", "Identity equipped successfully!").color(NamedTextColor.GREEN));
                }, null);
            }).exceptionally(ex -> {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.equip_failed", "Failed to equip identity: " + errorMessage, Component.text(errorMessage)).color(NamedTextColor.RED));
                return null;
            });
        }, null);
    }
}
