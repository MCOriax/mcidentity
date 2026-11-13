package io.github.mcoriax.mcidentity.bukkit.profession;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenProfessionGui;
import io.github.mcoriax.mcidentity.bukkit.event.MCProfessionEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCProfessionUnequipEvent;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.ApplyProfessionListName;
import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;

/**
 * Changes the profession a player wears.
 *
 * <p>The single entry point for {@code /identity wear}, {@code /identity unwear}
 * and the profession GUI. It checks the wearing permission, the swap cooldown
 * and any change already in flight, fires the cancellable
 * {@link MCProfessionEquipEvent} or {@link MCProfessionUnequipEvent}, stores the
 * change through {@link MCIdentityProvider}, logs it, and then refreshes the
 * action bar and tab list. The player's inventory is never touched: a
 * profession is something worn, not a separate character.</p>
 *
 * <p>Call it on the thread that owns the player.</p>
 */
public final class ProfessionWearing {

    private ProfessionWearing() {
    }

    /**
     * Puts on a profession.
     * @param plugin The plugin instance.
     * @param player The player wearing it.
     * @param requestedCode The profession's code as typed or clicked.
     * @param reopenGui Whether to reopen the profession GUI once done.
     */
    public static void wear(JavaPlugin plugin, Player player, String requestedCode, boolean reopenGui) {
        String code = requestedCode.trim().toLowerCase(Locale.ROOT);
        UUID playerUuid = player.getUniqueId();

        if (!MCIdentityManager.getRegisteredProfessions().containsKey(code)) {
            player.sendMessage(ChatColor.RED + MessageComponents.value(
                    "mcidentity.command.error.profession_not_found.prefix",
                    "Error: Profession '",
                    code,
                    "mcidentity.command.error.profession_not_found.suffix",
                    "' does not exist!"));
            return;
        }
        if (!Permissions.canWear(player, code)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to wear that profession!");
            return;
        }

        String previous = MCIdentityManager.getActiveProfession(playerUuid).orElse(BaseProfession.CODE);
        if (previous.equalsIgnoreCase(code)) {
            player.sendMessage(ChatColor.YELLOW + "You are already wearing that profession.");
            return;
        }
        if (!readyToChange(player)) {
            return;
        }

        MCProfessionEquipEvent event = new MCProfessionEquipEvent(player, previous, code);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        String displayName = MCIdentityManager.getProfessionDisplayName(code);
        apply(plugin, player, code, ActionType.EQUIP,
                ChatColor.GREEN + "You are now wearing " + displayName + ".", reopenGui);
    }

    /**
     * Takes off the worn profession, returning the player to the base profession.
     * @param plugin The plugin instance.
     * @param player The player.
     * @param reopenGui Whether to reopen the profession GUI once done.
     */
    public static void unwear(JavaPlugin plugin, Player player, boolean reopenGui) {
        String current = MCIdentityManager.getActiveProfession(player.getUniqueId()).orElse(BaseProfession.CODE);
        if (BaseProfession.isBaseProfession(current)) {
            player.sendMessage(ChatColor.RED + "You are not wearing a profession.");
            return;
        }
        if (!readyToChange(player)) {
            return;
        }

        MCProfessionUnequipEvent event = new MCProfessionUnequipEvent(player, current);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        apply(plugin, player, BaseProfession.CODE, ActionType.UNEQUIP,
                ChatColor.GREEN + "Profession removed. You are back to the base Player profession.", reopenGui);
    }

    /**
     * Checks the swap cooldown and any change already in flight.
     * @param player The player.
     * @return true when the player may change profession now.
     */
    private static boolean readyToChange(Player player) {
        UUID playerUuid = player.getUniqueId();
        if (MCIdentityManager.isEquipPending(playerUuid)) {
            player.sendMessage(ChatColor.RED + "A profession change is already in progress.");
            return false;
        }
        if (MCIdentityManager.isOnSwapCooldown(playerUuid)) {
            String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(playerUuid));
            player.sendMessage(ChatColor.RED + MessageComponents.value(
                    "mcidentity.command.error.swap_cooldown.prefix",
                    "You must wait ",
                    remaining,
                    "mcidentity.command.error.swap_cooldown.suffix",
                    " before changing profession!"));
            return false;
        }
        return true;
    }

    /**
     * Stores the change, logs it, and refreshes the player once it lands.
     * @param plugin The plugin instance.
     * @param player The player.
     * @param code The profession now worn.
     * @param actionType The action to log.
     * @param message The message sent on success.
     * @param reopenGui Whether to reopen the profession GUI.
     */
    private static void apply(JavaPlugin plugin, Player player, String code, ActionType actionType,
                              String message, boolean reopenGui) {
        UUID playerUuid = player.getUniqueId();
        MCIdentityManager.setEquipPending(playerUuid, true);
        MCIdentityProvider provider = MCIdentityProvider.instance;
        provider.setActiveProfession(playerUuid, code)
            .thenCompose(v -> provider.logAction(playerUuid, code, actionType))
            .whenComplete((v, ex) -> Schedulers.forEntity(player).run(plugin, st -> {
                MCIdentityManager.setEquipPending(playerUuid, false);
                if (ex != null) {
                    plugin.getLogger().warning("Failed to change the profession of " + player.getName()
                        + ": " + ex.getMessage());
                    player.sendMessage(ChatColor.RED + "Failed to change your profession. Please try again.");
                    return;
                }
                MCIdentityManager.setSwapCooldown(playerUuid);
                refresh(plugin, player);
                player.sendMessage(message);
                if (reopenGui) {
                    OpenProfessionGui.execute(plugin, player);
                }
            }, () -> MCIdentityManager.setEquipPending(playerUuid, false)));
    }

    /**
     * Redraws the action bar, health and food bars, and the tab list name.
     * @param plugin The plugin instance.
     * @param player The player.
     */
    public static void refresh(JavaPlugin plugin, Player player) {
        if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
            MCIdentityManager.getSession(player.getUniqueId())
                .ifPresent(session -> statsListener.updateVisuals(player, session));
        }
        ApplyProfessionListName.execute(plugin, player);
    }
}
