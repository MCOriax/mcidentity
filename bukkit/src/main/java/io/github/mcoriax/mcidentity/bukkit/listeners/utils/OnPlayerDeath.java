package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityDeathEvent;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class OnPlayerDeath {
    /**
     * Handle the player death event.
     * @param plugin The plugin instance.
     * @param event The player death event.
     */
    public static void execute(JavaPlugin plugin, PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().clone();
        UUID playerUuid = player.getUniqueId();

        // Default behaviour keeps the identity on death; only drop it when the
        // server opts in via config.yml. Read on the main thread here so the
        // async callbacks below capture a stable value.
        final boolean dropOnDeath = plugin.getConfig().getBoolean("drop_on_death", false);

        MCIdentityProvider.instance.getSession(playerUuid).thenAccept(optSession ->
                optSession.ifPresent(session -> {
                    UUID identityUuid = session.getIdentityUniqueId();

                    // Base identities — the primary and every alternative,
                    // whatever their profession — are permanently bound: they never
                    // drop, trade, or delete on death, only regular identities can.
                    // They are recognised by their deterministic UUID, so an
                    // alternative of any class is still protected.
                    boolean wearingBaseIdentity = BaseProfession.identityIndex(playerUuid, identityUuid,
                            MCIdentityManager.getCachedBaseIdentitySlots(playerUuid)) >= 0;
                    if (wearingBaseIdentity) {
                        return;
                    }

                    MCIdentityManager.clearSwapCooldown(playerUuid);
                    announceDeath(plugin, player, deathLocation, identityUuid, session.getContentKey(), dropOnDeath);
                }));
    }

    /**
     * Deserializes the worn identity item off the game thread, then fires the
     * death event back on the player's thread.
     * @param plugin The plugin instance.
     * @param player The player who died.
     * @param deathLocation Where the player died.
     * @param identityUuid The identity being worn.
     * @param base64 The serialized identity item, or null when there is none.
     * @param dropOnDeath The configured default for whether the identity drops.
     */
    private static void announceDeath(JavaPlugin plugin, Player player, Location deathLocation,
                                      UUID identityUuid, String base64, boolean dropOnDeath) {
        Schedulers.async().runNow(plugin, stAsync -> {
            try {
                ItemStack identityItem = base64 != null ? ItemStackSerializer.deserialize(base64) : null;
                Schedulers.forEntity(player).run(plugin, st1 -> {
                    MCIdentityDeathEvent deathEvent = new MCIdentityDeathEvent(player, identityUuid, identityItem);
                    deathEvent.setDrop(dropOnDeath);
                    Bukkit.getPluginManager().callEvent(deathEvent);

                    if (!deathEvent.isDrop()) {
                        // Identity is kept. The fatal blow left the
                        // identity's tracked health at 0, so restore
                        // current health, food, and energy to full for
                        // the respawn instead of leaving the visuals at
                        // 0%.
                        restoreCurrentStats(player, player.getUniqueId());
                        return;
                    }

                    dropIdentity(plugin, player, deathLocation, identityUuid, identityItem);
                }, null);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to process death event for " + player.getName());
                e.printStackTrace();
            }
        });
    }

    /**
     * Falls back to the base profession and drops the identity item where the
     * player died.
     * @param plugin The plugin instance.
     * @param player The player who died.
     * @param deathLocation Where the player died.
     * @param identityUuid The identity being dropped.
     * @param identityItem The identity item to drop, or null when there is none.
     */
    private static void dropIdentity(JavaPlugin plugin, Player player, Location deathLocation,
                                     UUID identityUuid, ItemStack identityItem) {
        // Dropping: fall back to the base profession so the
        // player is never left in the "no profession" state.
        EquipBaseProfession.execute(plugin, player, null,
                ChatColor.YELLOW + "You are back to the base Player profession.", false);

        if (identityItem == null) {
            return;
        }

        UUID playerUuid = player.getUniqueId();
        Schedulers.async().runNow(plugin, stDrop -> {
            Schedulers.forEntity(player).run(plugin, st2 ->
                    dropAtDeathLocation(plugin, player, deathLocation, identityItem), null);
            MCIdentityProvider.instance.logAction(playerUuid, identityUuid, ActionType.DEATH_DROP);
        });
    }

    /**
     * Drops the identity item into the world and tells the player, if they are
     * still online.
     * @param plugin The plugin instance.
     * @param player The player who died.
     * @param deathLocation Where the player died.
     * @param identityItem The identity item to drop.
     */
    private static void dropAtDeathLocation(JavaPlugin plugin, Player player, Location deathLocation, ItemStack identityItem) {
        if (deathLocation.getWorld() == null) {
            return;
        }
        try {
            deathLocation.getWorld().dropItemNaturally(deathLocation, identityItem);
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(ChatColor.RED + "Your identity has been dropped due to death!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to drop identity item for " + player.getName());
        }
    }

    /**
     * Restores the player's active identity current health, food, and energy
     * to their maximums and refreshes the visuals. Used when the identity is
     * kept on death so the player respawns at full instead of the 0% the fatal
     * blow left behind.
     * @param player The player who died.
     * @param playerUuid The UUID of the player.
     */
    private static void restoreCurrentStats(Player player, UUID playerUuid) {
        MCIdentityManager.getSession(playerUuid).ifPresent(session -> {
            MCIdentityManager.updateHealth(playerUuid, session.stats().getHealth());
            MCIdentityManager.updateFood(playerUuid, session.stats().getFood());
            MCIdentityManager.updateEnergy(playerUuid, session.stats().getEnergy());
            if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                MCIdentityManager.getSession(playerUuid).ifPresent(refreshed ->
                        statsListener.updateVisuals(player, refreshed));
            }
        });
    }
}
