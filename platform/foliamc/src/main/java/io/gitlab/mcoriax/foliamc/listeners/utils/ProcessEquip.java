package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.bukkit.event.IdentityEquipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
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
                plugin.setEquipPending(player.getUniqueId(), false);
                return;
            }
            
            IdentityEquipEvent equipEvent = new IdentityEquipEvent(player, identityUuid, itemToEquip);
            Bukkit.getPluginManager().callEvent(equipEvent);
            
            if (equipEvent.isCancelled()) {
                RefundItemSafely.execute(plugin, player, itemToEquip, "Equipping was cancelled.");
                return;
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                plugin.setEquipPending(player.getUniqueId(), false);
                plugin.setSwapCooldown(player.getUniqueId());
                player.getScheduler().run(plugin, innerTask -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;

                    // Trigger immediate visual update
                    MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
                        plugin.getStatsListener().updateVisuals(player, session);
                    });

                    player.sendMessage(Component.text("Identity equipped!", NamedTextColor.GREEN));
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                    
                    if (reopenGui) {
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }
                }, null);
            }).exceptionally(ex -> {
                plugin.setEquipPending(player.getUniqueId(), false);
                RefundItemSafely.execute(plugin, player, itemToEquip, "Failed to equip identity: " + ex.getMessage());
                return null;
            });
        }, null);
    }
}
