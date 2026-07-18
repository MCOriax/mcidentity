package io.github.mcoriax.mcidentity.bukkit.commands.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import org.bukkit.Bukkit;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FetchAndDisplayIdentityInfo {
    /**
     * Fetch identity information from the database and display it to the player.
     * @param plugin The plugin instance.
     * @param player The player to display info to.
     * @param uuid The identity UUID.
     */
    public static void execute(JavaPlugin plugin, Player player, UUID uuid) {
        MCIdentityProvider.instance.getIdentityKey(uuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                Schedulers.forEntity(player).run(plugin, st -> {
                    player.sendMessage(ChatColor.RED + "Identity data not found in database.");
                }, null);
                return;
            }

            IdentityKey key = optKey.get();
            
            CompletableFuture<Optional<IdentityProperties>> propsFuture = MCIdentityProvider.instance.getProperties(uuid);
            CompletableFuture<Optional<IdentityStats>> statsFuture = MCIdentityProvider.instance.getStats(uuid);
            
            CompletableFuture.allOf(propsFuture, statsFuture).thenAccept(v -> {
                Optional<IdentityProperties> optProps = propsFuture.join();
                Optional<IdentityStats> optStats = statsFuture.join();
                
                Schedulers.forEntity(player).run(plugin, st -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    if (optProps.isPresent() && optStats.isPresent()) {
                        RenderIdentityInfo.execute(player, uuid, key, optProps.get(), optStats.get());
                    } else {
                        player.sendMessage(ChatColor.RED + "Identity data is incomplete in database.");
                    }
                }, null);
            });
        });
    }
}
