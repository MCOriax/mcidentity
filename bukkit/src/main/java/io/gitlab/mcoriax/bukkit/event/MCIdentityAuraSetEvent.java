package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an identity's aura cosmetic is set or cleared.
 */
public class MCIdentityAuraSetEvent extends MCIdentityEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final String aura;
    private boolean cancelled;

    public MCIdentityAuraSetEvent(Player player, UUID identityUuid, @Nullable String aura) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.aura = aura;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getIdentityUuid() {
        return identityUuid;
    }

    /**
     * Gets the aura system code being set.
     * @return The aura code, or null if clearing.
     */
    public @Nullable String getAura() {
        return aura;
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
