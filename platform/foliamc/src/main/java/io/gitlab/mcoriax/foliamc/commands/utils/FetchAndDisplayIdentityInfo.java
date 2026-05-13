package io.gitlab.mcoriax.foliamc.commands.utils;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FetchAndDisplayIdentityInfo {
    /**
     * Fetch identity information from the database and display it to the player.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, UUID uuid) {
        MCIdentityProvider.instance.getIdentityKey(uuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    player.sendMessage(Component.translatable("mcidentity.command.error.identity_data_not_found", "Identity data not found in database.").color(NamedTextColor.RED));
                }, null);
                return;
            }

            IdentityKey key = optKey.get();
            
            CompletableFuture<Optional<IdentityProperties>> propsFuture = MCIdentityProvider.instance.getProperties(uuid);
            CompletableFuture<Optional<IdentityStats>> statsFuture = MCIdentityProvider.instance.getStats(uuid);
            
            CompletableFuture.allOf(propsFuture, statsFuture).thenAccept(v -> {
                Optional<IdentityProperties> optProps = propsFuture.join();
                Optional<IdentityStats> optStats = statsFuture.join();
                
                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    if (optProps.isPresent() && optStats.isPresent()) {
                        RenderIdentityInfo.execute(player, uuid, key, optProps.get(), optStats.get());
                    } else {
                        player.sendMessage(Component.translatable("mcidentity.command.error.identity_data_incomplete", "Identity data is incomplete in database.").color(NamedTextColor.RED));
                    }
                }, null);
            });
        });
    }
}
