package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.commands.MCIdentityCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Optional;

public class HandleRuneGuiClick {
    /**
     * Handle clicks in the rune GUI. The base "player" profession is
     * restricted from runes, so any click other than the back button is
     * blocked while it is equipped.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 45) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
            return;
        }

        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());
            if (session.isPresent() && BaseProfession.isBaseProfession(session.get().identityKey().getProfessionCode())) {
                player.sendMessage(ChatColor.RED + "The base Player profession cannot use runes!");
            }
        }
    }
}
