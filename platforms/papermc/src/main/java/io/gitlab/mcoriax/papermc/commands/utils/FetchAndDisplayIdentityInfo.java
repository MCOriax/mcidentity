package io.gitlab.mcoriax.papermc.commands.utils;

import org.bukkit.Bukkit;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public static void execute(MCIdentityPlugin plugin, Player player, UUID uuid) {
        MCIdentityProvider.instance.getIdentityKey(uuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    player.sendMessage(Component.translatable("mcidentity.command.error.identity_data_not_found", "Identity data not found in database.").color(NamedTextColor.RED));
                });
                return;
            }

            IdentityKey key = optKey.get();
            
            CompletableFuture<Optional<IdentityProperties>> propsFuture = MCIdentityProvider.instance.getProperties(uuid);
            CompletableFuture<Optional<IdentityStats>> statsFuture = MCIdentityProvider.instance.getStats(uuid);
            
            CompletableFuture.allOf(propsFuture, statsFuture).thenAccept(v -> {
                Optional<IdentityProperties> optProps = propsFuture.join();
                Optional<IdentityStats> optStats = statsFuture.join();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    if (optProps.isPresent() && optStats.isPresent()) {
                        RenderIdentityInfo.execute(player, uuid, key, optProps.get(), optStats.get());
                    } else {
                        player.sendMessage(Component.translatable("mcidentity.command.error.identity_data_incomplete", "Identity data is incomplete in database.").color(NamedTextColor.RED));
                    }
                });
            });
        });
    }
}


