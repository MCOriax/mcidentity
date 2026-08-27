package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityCreationEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Equips a player's permanent base "player" profession.
 *
 * <p>This is the single entry point for the base profession fallback: first
 * join, death drops, unequips, and failed equips all route through here so a
 * player is never left without a profession. The base identity is created on
 * demand, keyed by the player's own UUID, and its storage restore is sequenced
 * before any returned identity item so the item lands inside the restored
 * inventory instead of being overwritten by it.</p>
 */
public class EquipBaseProfession {

    /**
     * Equips the base profession with no item to return and no message.
     * @param plugin The plugin instance.
     * @param player The player falling back to the base profession.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        execute(plugin, player, null, null, false);
    }

    /**
     * Equips the base profession for a player, creating the base identity in
     * the database on first use.
     * @param plugin The plugin instance.
     * @param player The player falling back to the base profession.
     * @param itemToReturn An identity item handed back after the base storage
     *                     is restored, or null.
     * @param message A message sent once the base profession is equipped, or null.
     * @param reopenGui Whether to reopen the identity GUI afterwards.
     */
    public static void execute(JavaPlugin plugin, Player player, ItemStack itemToReturn, String message, boolean reopenGui) {
        UUID playerUuid = player.getUniqueId();
        UUID baseUuid = BaseProfession.identityUuid(playerUuid);

        ensureBaseIdentityExists(plugin, player)
            .thenCompose(v -> MCIdentityProvider.instance.setActiveIdentity(playerUuid, baseUuid, null))
            .thenAccept(v -> Schedulers.forEntity(player).run(plugin, st -> {
                MCIdentityEquipEvent equipEvent = new MCIdentityEquipEvent(player, baseUuid, null);
                Bukkit.getPluginManager().callEvent(equipEvent);

                MCIdentityManager.getSession(playerUuid).ifPresentOrElse(session -> {
                    restoreStorageThenReturnItem(plugin, player,
                        session.properties().getInventoryContents(),
                        session.properties().getEnderchestContents(),
                        itemToReturn);
                    if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                        statsListener.updateVisuals(player, session);
                    }
                }, () -> restoreStorageThenReturnItem(plugin, player, null, null, itemToReturn));

                if (message != null) {
                    player.sendMessage(message);
                }
                if (reopenGui) {
                    new MCIdentityCommand(plugin).openIdentityGui(player);
                }
            }, null))
            .exceptionally(ex -> {
                plugin.getLogger().severe("Failed to equip the base profession for " + player.getName() + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Creates the player's base identity if it does not exist yet. The base
     * identity shares the player's UUID, which binds it permanently: it is
     * never deleted and never converted into an item.
     * @param plugin The plugin instance.
     * @param player The player owning the base identity.
     * @return A CompletableFuture that completes once the identity exists.
     */
    private static CompletableFuture<Void> ensureBaseIdentityExists(JavaPlugin plugin, Player player) {
        UUID baseUuid = BaseProfession.identityUuid(player.getUniqueId());
        return MCIdentityProvider.instance.getIdentityKey(baseUuid).thenCompose(optKey -> {
            if (optKey.isPresent()) {
                return CompletableFuture.completedFuture(null);
            }

            IdentityKey key = new IdentityKey() {
                @Override public UUID getUniqueId() { return baseUuid; }
                @Override public String getProfessionCode() { return BaseProfession.CODE; }
            };

            MutableIdentityProperties props = new MutableIdentityProperties(null, 1, 0, 0, null, null, null);

            Map<String, Integer> baseStats = MCIdentityManager.getProfessionBaseStats(BaseProfession.CODE);
            final int energyVal = baseStats.getOrDefault("energy", 100);
            final int healthVal = baseStats.getOrDefault("health", 20);
            final int foodVal = baseStats.getOrDefault("food", 20);
            final int strengthVal = baseStats.getOrDefault("strength", 0);
            final int defenseVal = baseStats.getOrDefault("defense", 0);
            final int wisdomVal = baseStats.getOrDefault("wisdom", 0);
            final int luckVal = baseStats.getOrDefault("luck", 0);
            final int agilityVal = baseStats.getOrDefault("agility", 0);
            final int resilienceVal = baseStats.getOrDefault("resilience", 0);

            IdentityStats stats = new IdentityStats() {
                @Override public int getEnergy() { return energyVal; }
                @Override public int getHealth() { return healthVal; }
                @Override public int getFood() { return foodVal; }
                @Override public int getStrength() { return strengthVal; }
                @Override public int getDefense() { return defenseVal; }
                @Override public int getWisdom() { return wisdomVal; }
                @Override public int getLuck() { return luckVal; }
                @Override public int getAgility() { return agilityVal; }
                @Override public int getResilience() { return resilienceVal; }
            };

            return MCIdentityProvider.instance.createIdentity(key, props, stats).thenRun(() ->
                Schedulers.forEntity(player).run(plugin, st -> {
                    MCIdentityCreationEvent event = new MCIdentityCreationEvent(key, props, stats);
                    Bukkit.getPluginManager().callEvent(event);
                }, null));
        });
    }

    /**
     * Restores the base identity's stored inventory and enderchest, then hands
     * back the given identity item into the restored inventory. A base
     * identity with no stored inventory yet (first join) adopts the player's
     * current items instead of clearing them.
     * @param plugin The plugin instance.
     * @param player The player.
     * @param invData The serialized inventory, or null to keep the current one.
     * @param ecData The serialized enderchest, or null to keep the current one.
     * @param itemToReturn An identity item handed back after the restore, or null.
     */
    private static void restoreStorageThenReturnItem(JavaPlugin plugin, Player player, String invData, String ecData, ItemStack itemToReturn) {
        Schedulers.async().runNow(plugin, stAsync -> {
            ItemStack[] inv = null;
            ItemStack[] ec = null;
            try {
                inv = deserializeOrNull(invData);
                ec = deserializeOrNull(ecData);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                    "Failed to load base profession storage for " + player.getName(), e);
            }

            final ItemStack[] finalInv = inv;
            final ItemStack[] finalEc = ec;
            Schedulers.forEntity(player).run(plugin, st ->
                applyStorage(player, finalInv, finalEc, itemToReturn), null);
        });
    }

    /**
     * Reads one serialized storage, treating a missing or empty value as "keep
     * whatever the player already has".
     * @param data The serialized contents, or null.
     * @return The contents, or null when there are none to restore.
     * @throws IOException When the stored contents cannot be read.
     */
    private static ItemStack[] deserializeOrNull(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return null;
        }
        return ItemStackSerializer.deserializeArray(data);
    }

    /**
     * Writes the restored storage back and hands the identity item over,
     * dropping whatever no longer fits.
     * @param player The player.
     * @param inv The inventory to restore, or null to keep the current one.
     * @param ec The enderchest to restore, or null to keep the current one.
     * @param itemToReturn An identity item handed back after the restore, or null.
     */
    private static void applyStorage(Player player, ItemStack[] inv, ItemStack[] ec, ItemStack itemToReturn) {
        if (inv != null) {
            player.getInventory().setContents(inv);
        }
        if (ec != null) {
            player.getEnderChest().setContents(ec);
        }
        if (itemToReturn != null) {
            player.getInventory().addItem(itemToReturn).values()
                .forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        }
    }
}
