package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenPlayerIdentityGui;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Optional;

public class HandleIdentityGuiClick {
    /**
     * Handle clicks in the identity GUI.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param textureKey The NamespacedKey for texture.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);

            Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());
            boolean wearingBase = session.isPresent()
                && BaseProfession.isBaseProfession(session.get().identityKey().getProfessionCode());

            if (slot == 0) {
                OpenPlayerIdentityGui.execute(plugin, player, 1);
                return;
            }

            if (slot == 6) {
                if (session.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must be wearing an identity to open the Rune GUI!");
                    return;
                }
                // The base profession is restricted from runes.
                if (wearingBase) {
                    player.sendMessage(ChatColor.RED + "The base Player profession cannot use runes!");
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityRuneGui(player);
                }
                return;
            }

            if (slot == 2) {
                if (session.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must be wearing an identity to open the Ability GUI!");
                    return;
                }
                // The base profession is restricted from abilities.
                if (wearingBase) {
                    player.sendMessage(ChatColor.RED + "The base Player profession cannot use abilities!");
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityAbilityGui(player);
                }
                return;
            }

            if (slot == 4) {
                HandleSlot4Click.execute(plugin, event, player, identityKey, professionKey, unidentifiedKey, textureKey);
                return;
            }

            if (slot == 8) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must be wearing an identity to open the Cosmetic GUI!");
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openCosmeticGui(player);
                }
                return;
            }
        } else {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }
}
