package io.github.mcoriax.mcidentity.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called on the player's thread before a player puts a profession on.
 *
 * <p>Nothing has changed yet: {@link #getPreviousProfession()} is still worn
 * and cancelling the event keeps it on. The player's data stays keyed by their
 * player UUID whatever they wear, so a listener only needs this event to react
 * to the profession itself, never to move data.</p>
 */
public class MCProfessionEquipEvent extends MCIdentityEvent implements Cancellable {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The player changing profession.
     */
    private final Player player;

    /**
     * The profession worn until now; {@code player} when none.
     */
    private final String previousProfession;

    /**
     * The profession being put on.
     */
    private final String professionCode;

    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;

    /**
     * Creates the event.
     * @param player The player changing profession.
     * @param previousProfession The profession worn until now.
     * @param professionCode The profession being put on.
     */
    public MCProfessionEquipEvent(Player player, String previousProfession, String professionCode) {
        this.player = player;
        this.previousProfession = previousProfession;
        this.professionCode = professionCode;
    }

    /**
     * Gets the player changing profession.
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the profession worn until now.
     * @return Its system code; {@code player} when none.
     */
    public String getPreviousProfession() {
        return previousProfession;
    }

    /**
     * Gets the profession being put on.
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
