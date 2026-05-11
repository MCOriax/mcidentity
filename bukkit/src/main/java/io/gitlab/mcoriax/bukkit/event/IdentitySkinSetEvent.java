package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an identity's skin cosmetic is set or cleared.
 */
public class IdentitySkinSetEvent extends MCIdentityEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final String skin;
    private boolean cancelled;

    public IdentitySkinSetEvent(Player player, UUID identityUuid, @Nullable String skin) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.skin = skin;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getIdentityUuid() {
        return identityUuid;
    }

    /**
     * Gets the skin Base64 string being set.
     * @return The skin texture, or null if clearing.
     */
    public @Nullable String getSkin() {
        return skin;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
