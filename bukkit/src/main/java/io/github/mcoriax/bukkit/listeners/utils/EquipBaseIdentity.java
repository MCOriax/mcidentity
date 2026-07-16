package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.bukkit.util.MessageComponents;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Switches a player onto one of their base "player" identities, selected by
 * index from the Player Identity GUI.
 *
 * <p>Switching is only allowed between base identities: a regular profession
 * identity must be unequipped by hand first so its key item is handed back
 * through the usual flow. The identity being swapped out receives a normal
 * {@link MCIdentityUnequipEvent}, so its inventory and enderchest are saved by
 * the inventory listener; the incoming identity receives a normal
 * {@link MCIdentityEquipEvent}, which restores its own storage. The primary
 * identity (index 0) is equipped through {@link EquipBaseProfession}, which
 * also creates it on demand.</p>
 */
public class EquipBaseIdentity {

    /**
     * Switches the player to the base identity at the given index.
     * Must be called on the player's entity thread (e.g. from a GUI click).
     * @param plugin The plugin instance.
     * @param player The player switching identities.
     * @param index The base identity index, 0 for the primary identity.
     */
    public static void execute(JavaPlugin plugin, Player player, int index) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = BaseProfession.identityUuid(playerUuid, index);

        Optional<ActiveIdentitySession> current = MCIdentityManager.getSession(playerUuid);

        if (current.isPresent() && current.get().identityUuid().equals(targetUuid)) {
            player.sendMessage(ChatColor.RED + "This identity is already equipped!");
            return;
        }

        // Regular identities carry a key item that must be handed back through
        // the normal unequip flow; only base identities — the primary and every
        // alternative, of any profession — may be switched here.
        if (current.isPresent() && !MCIdentityManager.isWearingBaseIdentity(playerUuid)) {
            player.sendMessage(ChatColor.RED + "Please unequip your current identity first!");
            return;
        }

        if (MCIdentityManager.isOnSwapCooldown(playerUuid)) {
            String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(playerUuid));
            player.sendMessage(ChatColor.RED + MessageComponents.value(
                    "mcidentity.command.error.swap_cooldown.prefix",
                    "You must wait ",
                    remaining,
                    "mcidentity.command.error.swap_cooldown.suffix",
                    " before swapping identities!"
            ));
            return;
        }

        if (MCIdentityManager.isEquipPending(playerUuid)) {
            return;
        }

        if (current.isPresent()) {
            MCIdentityUnequipEvent unequipEvent = new MCIdentityUnequipEvent(player, current.get().identityUuid());
            Bukkit.getPluginManager().callEvent(unequipEvent);
            if (unequipEvent.isCancelled()) {
                return;
            }
        }

        MCIdentityManager.setSwapCooldown(playerUuid);

        // Close the GUI so no further switches can be clicked while this one
        // is in flight. Closing is scheduled: closing an inventory from inside
        // its own click event is unsafe.
        Schedulers.forEntity(player).run(plugin, st -> player.closeInventory(), null);

        if (index <= BaseProfession.PRIMARY_INDEX) {
            EquipBaseProfession.execute(plugin, player, null,
                    ChatColor.GREEN + "You are now playing as your Primary identity.", false);
            return;
        }

        MCIdentityManager.setEquipPending(playerUuid, true);
        MCIdentityProvider.instance.getIdentityKey(targetUuid)
            .thenCompose(optKey -> {
                if (optKey.isEmpty()) {
                    return CompletableFuture.failedFuture(
                        new IllegalStateException("Alternative identity " + index + " is not registered"));
                }
                return MCIdentityProvider.instance.setActiveIdentity(playerUuid, targetUuid, null);
            })
            .thenAccept(v -> Schedulers.forEntity(player).run(plugin, st -> {
                MCIdentityManager.setEquipPending(playerUuid, false);

                MCIdentityEquipEvent equipEvent = new MCIdentityEquipEvent(player, targetUuid, null);
                Bukkit.getPluginManager().callEvent(equipEvent);

                MCIdentityManager.getSession(playerUuid).ifPresent(session -> {
                    if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                        statsListener.updateVisuals(player, session);
                    }
                });

                player.sendMessage(ChatColor.GREEN + "You are now playing as Alternative Identity #" + index + ".");
            }, null))
            .exceptionally(ex -> {
                MCIdentityManager.setEquipPending(playerUuid, false);
                plugin.getLogger().severe("Failed to switch " + player.getName()
                    + " to base identity " + index + ": " + ex.getMessage());
                Schedulers.forEntity(player).run(plugin, st ->
                    player.sendMessage(ChatColor.RED + "Failed to switch identity. Please try again."), null);
                return null;
            });
    }
}
