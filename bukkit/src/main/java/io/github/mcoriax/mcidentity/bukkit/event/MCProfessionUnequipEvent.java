package io.github.mcoriax.mcidentity.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called on the player's thread before a player takes their profession off and
 * goes back to the base {@code player} profession.
 *
 * <p>Nothing has changed yet and cancelling the event keeps the profession on.
 * Putting another profession on directly fires {@link MCProfessionEquipEvent}
 * instead, carrying the profession taken off as its previous profession.</p>
 */
public class MCProfessionUnequipEvent extends MCIdentityEvent implements Cancellable {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The player taking the profession off.
     */
    private final Player player;

    /**
     * The profession being taken off.
     */
    private final String professionCode;

    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;

    /**
     * Creates the event.
     * @param player The player taking the profession off.
     * @param professionCode The profession being taken off.
     */
    public MCProfessionUnequipEvent(Player player, String professionCode) {
        this.player = player;
        this.professionCode = professionCode;
    }

    /**
     * Gets the player taking the profession off.
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the profession being taken off.
     * @return Its system code.
     */
    public String getProfessionCode() {
        return professionCode;
    }

    /**
     * Gets whether this event has been cancelled by another plugin.
     * @return True when the change will not be carried out.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether this event is cancelled.
     * @param cancel True to stop the change from being carried out.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the handlers for this event.
     * @return The handler list.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event.
     * @return The handler list.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
