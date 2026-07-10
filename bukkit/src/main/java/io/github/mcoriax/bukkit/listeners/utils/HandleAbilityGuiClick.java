package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.api.model.EquippedAbility;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.bukkit.commands.utils.CreateAbilityItem;
import io.github.mcoriax.bukkit.scheduler.Schedulers;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class HandleAbilityGuiClick {

    /**
     * Maps raw GUI slots to the logical ability slot they represent
     * (slot 10 is the ultimate slot).
     */
    private static final Map<Integer, Integer> ABILITY_SLOTS = Map.of(
        10, 1, 12, 2, 14, 3, 16, 4, 29, 5,
        31, 6, 33, 7, 48, 8, 50, 9, 53, 10
    );

    /**
     * Handle clicks in the ability GUI. Clicking an ability slot while
     * holding an ability item on the cursor equips it: the MythicMobs
     * ability code and the skin are read from the item's persistent data
     * container and stored for the slot. The base "player" profession is
     * restricted from abilities, so any click other than the back button is
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
            if (session.isEmpty()) {
                return;
            }
            if (BaseProfession.isBaseProfession(session.get().identityKey().getProfessionCode())) {
                player.sendMessage(ChatColor.RED + "The base Player profession cannot use abilities!");
                return;
            }

            Integer logicSlot = ABILITY_SLOTS.get(slot);
            if (logicSlot == null) {
                return;
            }

            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType() == Material.AIR || !cursor.hasItemMeta()) {
                return;
            }

            PersistentDataContainer container = cursor.getItemMeta().getPersistentDataContainer();
            String abilityCode = container.get(
                new NamespacedKey(plugin, CreateAbilityItem.ABILITY_CODE_KEY), PersistentDataType.STRING);
            if (abilityCode == null) {
                return;
            }
            String abilitySkin = container.get(
                new NamespacedKey(plugin, CreateAbilityItem.ABILITY_SKIN_KEY), PersistentDataType.STRING);

            // Consume one ability item from the cursor before the write.
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
            } else {
                event.getView().setCursor(null);
            }

            UUID identityUuid = session.get().identityUuid();
            String skinShown = new EquippedAbility(abilityCode, abilitySkin).abilitySkinOrDefault();
            MCIdentityProvider.instance.setEquippedAbility(identityUuid, logicSlot, abilityCode, abilitySkin)
                .thenRun(() -> Schedulers.forEntity(player).run(plugin, st -> {
                    player.sendMessage(ChatColor.GREEN + "Equipped ability " + ChatColor.WHITE + abilityCode
                        + ChatColor.GREEN + " (skin " + ChatColor.WHITE + skinShown + ChatColor.GREEN
                        + ") into slot " + logicSlot + ".");
                    MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                    if (cmd != null) {
                        cmd.openIdentityAbilityGui(player);
                    }
                }, null));
        }
    }
}
