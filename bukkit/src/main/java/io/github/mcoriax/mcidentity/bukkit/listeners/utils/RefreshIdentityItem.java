package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.bukkit.util.IdentityItemRenderer;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Rewrites the name and lore of the identity item a player is currently
 * wearing, from that identity's own alias, profession, and level.
 *
 * <p>While an identity is worn its item exists only as the session's stored
 * copy — it is handed back to the player when the identity is unequipped, and
 * the Main Identity GUI renders that same stored copy in its identity slot. So
 * refreshing the stored copy is what refreshes both.</p>
 *
 * <p>This runs when an alias is claimed and when an identity is equipped.
 * Without the first, an item kept the name it was built with and only caught up
 * on the next level up; without the second, an item created before the alias
 * naming existed would never be re-titled at all.</p>
 *
 * <p>Nothing here reads the item's existing name or lore — both are composed
 * from the session and overwritten, so a player who renamed their item simply
 * has that name replaced the next time their identity is rendered.</p>
 */
public final class RefreshIdentityItem {

    /**
     * Prevents instantiation of this utility holder.
     */
    private RefreshIdentityItem() {
    }

    /**
     * Rebuilds the worn identity item's presentation and stores it back.
     * @param plugin The plugin instance.
     * @param player The player whose worn identity item is refreshed.
     * @return A future completing once the refreshed item has been stored, so a
     *         caller that then shows the item can wait for it. It completes
     *         immediately when there is nothing to refresh, and never completes
     *         exceptionally — a failure is logged and the old item kept.
     */
    public static CompletableFuture<Void> execute(JavaPlugin plugin, Player player) {
        ActiveIdentitySession session = MCIdentityManager.getSession(player.getUniqueId()).orElse(null);
        if (session == null) {
            return CompletableFuture.completedFuture(null);
        }
        String base64 = session.contentKey();
        if (base64 == null || base64.isEmpty()) {
            // A base identity has no item of its own; nothing to re-title.
            return CompletableFuture.completedFuture(null);
        }

        // Deserializing and re-serializing an ItemStack is comparatively heavy,
        // so it stays off the thread that owns the player.
        CompletableFuture<Void> done = new CompletableFuture<>();
        Schedulers.async().runNow(plugin, task -> {
            try {
                ItemStack item = ItemStackSerializer.deserialize(base64);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    done.complete(null);
                    return;
                }
                IdentityItemRenderer.apply(meta, session.identityUuid(), session.properties().getAlias(),
                    session.identityKey().getProfessionCode(), session.properties().getLevel());
                item.setItemMeta(meta);
                MCIdentityProvider.instance
                    .updateSessionContentKey(player.getUniqueId(), ItemStackSerializer.serialize(item))
                    .whenComplete((v, ex) -> done.complete(null));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                    "Failed to refresh the identity item for " + player.getName(), e);
                done.complete(null);
            }
        });
        return done;
    }
}
