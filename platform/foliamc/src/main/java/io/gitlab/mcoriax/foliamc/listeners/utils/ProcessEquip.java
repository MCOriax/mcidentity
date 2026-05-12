package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.bukkit.event.IdentityEquipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityStatsListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ProcessEquip {
    /**
     * Process the equipment of an identity.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, UUID identityUuid, ItemStack itemToEquip, String base64, boolean reopenGui) {
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                return;
            }
            
            IdentityEquipEvent equipEvent = new IdentityEquipEvent(player, identityUuid, itemToEquip);
            Bukkit.getPluginManager().callEvent(equipEvent);
            
            if (equipEvent.isCancelled()) {
                RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.equip_cancelled", "Equipping was cancelled.").color(NamedTextColor.RED));
                return;
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                MCIdentityManager.setSwapCooldown(player.getUniqueId());
                player.getScheduler().run(plugin, innerTask -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;

                    // Trigger immediate visual update
                    MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
                        ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).updateVisuals(player, session);
                    });

                    player.sendMessage(Component.translatable("mcidentity.status.equipped", "Identity equipped!").color(NamedTextColor.GREEN));
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                    
                    if (reopenGui) {
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }
                }, null);
            }).exceptionally(ex -> {
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.equip_failed", "Failed to equip identity: {0}", Component.text(ex.getMessage() != null ? ex.getMessage() : "Unknown error")).color(NamedTextColor.RED));
                return null;
            });
        }, null);
    }
}
