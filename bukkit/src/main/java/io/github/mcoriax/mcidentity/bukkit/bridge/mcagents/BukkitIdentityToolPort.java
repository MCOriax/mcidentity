package io.github.mcoriax.mcidentity.bukkit.bridge.mcagents;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.tools.IdentitySnapshot;
import io.github.mcoriax.mcidentity.tools.IdentityToolPort;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serves the identity tools from the RAM session cache.
 *
 * <p>Every read goes through {@link MCIdentityManager}, whose state holders are
 * concurrent and safe to read from any thread, so a tool call never reaches the
 * database and never blocks. The only server call is the online lookup, which
 * resolves the player by UUID and reads nothing from the entity, so it is safe
 * off the entity's owning thread on Folia as well.</p>
 */
public final class BukkitIdentityToolPort implements IdentityToolPort {

    /**
     * The owning plugin; the port answers nothing once it is disabled.
     */
    private final Plugin plugin;

    /**
     * Creates the port.
     * @param plugin The owning plugin.
     */
    public BukkitIdentityToolPort(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Boolean> isOnlinePlayer(UUID playerUuid) {
        if (playerUuid == null || !plugin.isEnabled()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(Bukkit.getPlayer(playerUuid) != null);
    }

    @Override
    public CompletableFuture<Optional<IdentitySnapshot>> activeIdentity(UUID playerUuid) {
        if (playerUuid == null || !plugin.isEnabled()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(MCIdentityManager.getSession(playerUuid).map(this::snapshot));
    }

    /**
     * Copies a cached session into plain data.
     * @param session The cached session.
     * @return The snapshot.
     */
    private IdentitySnapshot snapshot(ActiveIdentitySession session) {
        String code = session.identityKey().getProfessionCode();
        IdentityProperties props = session.properties();
        int maxLevel;
        int expPerLevel;
        if (BaseProfession.CODE.equalsIgnoreCase(code)) {
            maxLevel = BaseProfession.MAX_LEVEL;
            expPerLevel = BaseProfession.expPerLevel(session.playerUuid(), session.identityUuid());
        } else {
            MCIdentityManager.ProfessionLevelConfig levels = MCIdentityManager.getProfessionLevelConfig(code);
            maxLevel = levels.maxLevel();
            expPerLevel = levels.expUp();
        }
        return new IdentitySnapshot(
            String.valueOf(session.identityUuid()),
            code,
            MCIdentityManager.getProfessionDisplayName(code),
            props.getAlias(),
            MCIdentityManager.isWearingBaseIdentity(session.playerUuid()),
            props.getLevel(),
            maxLevel,
            props.getExp(),
            expPerLevel,
            props.getStatPoints(),
            stats(session.stats()),
            session.currentHealth(),
            session.currentEnergy(),
            session.currentFood());
    }

    /**
     * Lists an identity's stats by display name, in the order the plugin shows them.
     * @param stats The stats.
     * @return The stats by name.
     */
    private static Map<String, Integer> stats(IdentityStats stats) {
        Map<String, Integer> named = new LinkedHashMap<>();
        named.put("Health", stats.getHealth());
        named.put("Energy", stats.getEnergy());
        named.put("Strength", stats.getStrength());
        named.put("Defense", stats.getDefense());
        named.put("Wisdom", stats.getWisdom());
        named.put("Luck", stats.getLuck());
        named.put("Agility", stats.getAgility());
        named.put("Resilience", stats.getResilience());
        named.put("Food", stats.getFood());
        return named;
    }
}
