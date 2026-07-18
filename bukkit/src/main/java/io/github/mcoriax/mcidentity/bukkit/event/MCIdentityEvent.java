package io.github.mcoriax.mcidentity.bukkit.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * Base class for all MCIdentity related events.
 * Supports Folia's asynchronous execution context.
 */
public abstract class MCIdentityEvent extends Event {

    /**
     * Constructor for MCIdentityEvent.
     * Automatically determines if the event is asynchronous based on the current thread.
     */
    protected MCIdentityEvent() {
        super(!Bukkit.isPrimaryThread());
    }
}
