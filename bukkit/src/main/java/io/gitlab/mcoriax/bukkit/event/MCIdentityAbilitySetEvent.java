package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an identity's ability is set or cleared.
 */
public class MCIdentityAbilitySetEvent extends MCIdentityEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final int slot;
    private final String abilityCode;
    private boolean cancelled;

    public MCIdentityAbilitySetEvent(Player player, UUID identityUuid, int slot, @Nullable String abilityCode) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.slot = slot;
        this.abilityCode = abilityCode;
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
     * Gets the ability code being set.
     * @return The ability code, or null if clearing the slot.
     */
    public @Nullable String getAbilityCode() {
        return abilityCode;
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
