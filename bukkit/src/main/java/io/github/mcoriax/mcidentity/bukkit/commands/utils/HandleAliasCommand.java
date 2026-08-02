package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.RefreshIdentityItem;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.IdentityAlias;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/**
 * Handles the {@code /identity alias} sub-command, which names the identity the
 * player is currently wearing.
 *
 * <p>Two forms are accepted:</p>
 * <ul>
 *   <li>{@code /identity alias} — shows the alias of the active identity.</li>
 *   <li>{@code /identity alias &lt;name&gt;} — claims that alias for the active
 *   identity.</li>
 * </ul>
 *
 * <p>An alias may contain only alphanumeric characters and is unique across
 * every identity on the network. Two aliases differing only in case are the
 * same alias, so a name already taken is refused whatever its casing, while the
 * value stored keeps the casing the player typed.</p>
 *
 * <p>Two identities are never renamed here:</p>
 * <ul>
 *   <li>An identity that already carries an alias. A name is claimed once and
 *   kept, so the player everyone knows does not change underneath them.</li>
 *   <li>The base {@code player} profession, which always goes by the player's
 *   Minecraft name.</li>
 * </ul>
 */
public class HandleAliasCommand {

    /**
     * Executes the alias sub-command.
     * @param plugin The plugin instance.
     * @param player The player who issued the command.
     * @param args The full command arguments ("alias", optional name).
     */
    public static void execute(JavaPlugin plugin, Player player, String[] args) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not currently wearing an identity!");
            return;
        }
        ActiveIdentitySession session = optSession.get();

        boolean baseProfession = BaseProfession.isBaseProfession(session.identityKey().getProfessionCode());
        String current = session.properties().getAlias();
        boolean hasAlias = current != null && !current.isBlank();

        if (args.length < 2) {
            if (baseProfession) {
                player.sendMessage(ChatColor.GRAY + "The base " + BaseProfession.DISPLAY_NAME
                    + " identity always goes by your Minecraft name, so it has no alias.");
            } else if (hasAlias) {
                player.sendMessage(ChatColor.GREEN + "Your identity alias is " + ChatColor.YELLOW + current);
            } else {
                player.sendMessage(ChatColor.GRAY + "Your identity has no alias yet. Set one with /identity alias <name>");
            }
            return;
        }

        // The base "player" identity is the player's own character rather than a
        // role they put on, so it is never renamed.
        if (baseProfession) {
            player.sendMessage(ChatColor.RED + "The base " + BaseProfession.DISPLAY_NAME
                + " identity always goes by your Minecraft name and cannot be given an alias!");
            return;
        }

        // An alias is claimed once and kept. Refusing here rather than in the
        // backend keeps the name a player is known by stable, and leaves the
        // one already claimed untouched.
        if (hasAlias) {
            player.sendMessage(ChatColor.RED + "This identity alias is already set as " + ChatColor.YELLOW + current);
            return;
        }

        String alias = args[1];
        if (!IdentityAlias.isValid(alias)) {
            player.sendMessage(ChatColor.RED + "An alias may only contain letters and numbers, and may be at most "
                    + IdentityAlias.MAX_LENGTH + " characters long!");
            return;
        }

        UUID identityUuid = session.identityUuid();
        MCIdentityProvider.instance.setIdentityAlias(identityUuid, alias).thenAccept(applied ->
            Schedulers.forEntity(player).run(plugin, st -> {
                if (applied) {
                    // The alias is already in the session cache by now, so the
                    // item is re-titled from it straight away rather than
                    // waiting for the next level up to rebuild it.
                    RefreshIdentityItem.execute(plugin, player);
                    player.sendMessage(ChatColor.GREEN + "Your identity alias is now " + ChatColor.YELLOW + alias);
                } else {
                    player.sendMessage(ChatColor.RED + "The alias '" + alias + "' is already taken!");
                }
            }, null));
    }
}
