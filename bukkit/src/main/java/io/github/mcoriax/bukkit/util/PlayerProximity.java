package io.github.mcoriax.bukkit.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Helper for attributing an unowned event to the closest nearby player.
 *
 * <p>Some Bukkit events (building an iron golem, summoning the Wither, ...) do not
 * expose the player that caused them. EXP listeners use this helper to credit the
 * nearest player within a small radius of the spawn location, which is the
 * conventional and self-limiting heuristic for these constructed entities. The
 * search is intentionally scoped to the spawn world only, so distance comparisons
 * never cross worlds.</p>
 */
public final class PlayerProximity {

    private PlayerProximity() {
        // Utility class; not meant to be instantiated.
    }

    /**
     * Finds the closest player to a location within a maximum radius.
     * @param center The reference location.
     * @param radius The maximum search radius in blocks.
     * @return The nearest player within the radius, or null if there is none.
     */
    public static Player findNearest(Location center, double radius) {
        if (center == null) {
            return null;
        }
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        double bestDistanceSquared = radius * radius;
        Player nearest = null;
        for (Player player : world.getPlayers()) {
            // Same-world guarantee lets us compare squared distances safely and cheaply.
            double distanceSquared = player.getLocation().distanceSquared(center);
            if (distanceSquared <= bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                nearest = player;
            }
        }
        return nearest;
    }
}
