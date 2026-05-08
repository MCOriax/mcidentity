package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an identity's rune is set or cleared.
 */
public class IdentityRuneSetEvent extends MCIdentityEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final int slot;
    private final String runeCode;
    private boolean cancelled;

    public IdentityRuneSetEvent(Player player, UUID identityUuid, int slot, @Nullable String runeCode) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.slot = slot;
        this.runeCode = runeCode;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getIdentityUuid() {
        return identityUuid;
    }

    public int getSlot() {
        return slot;
    }

    /**
     * Gets the rune code being set.
     * @return The rune code, or null if clearing the slot.
     */
    public @Nullable String getRuneCode() {
        return runeCode;
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
