package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles magic and economy related EXP actions:
 * {@code enchant_item}, {@code villager_trade} and {@code repair_item}.
 *
 * <p>The listener only detects the action and forwards it to the
 * {@link ExperienceManager}. Whether the player's active identity actually
 * gains EXP is decided entirely by the profession YAML configuration.</p>
 */
public class MagicAndEconomyExpListener implements Listener {

    /** Result slot index of both merchant and anvil inventories. */
    private static final int RESULT_SLOT = 2;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new MagicAndEconomyExpListener.
     * @param plugin The plugin instance.
     */
    public MagicAndEconomyExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player enchants an item at an enchanting table.
     * @param event The enchant item event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        Player player = event.getEnchanter();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Keyed by the enchanted item material so configs can reward specific items.
        exp.awardExp(player, ExpAction.ENCHANT_ITEM, item.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player collects the result of a villager trade or an anvil operation.
     * @param event The inventory click event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType != InventoryType.MERCHANT && topType != InventoryType.ANVIL) {
            return;
        }

        // Only react to the player actually taking the produced result item.
        if (event.getRawSlot() != RESULT_SLOT || event.getAction() == InventoryAction.NOTHING) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        if (topType == InventoryType.MERCHANT) {
            exp.awardExp(player, ExpAction.VILLAGER_TRADE, result.getType().name().toLowerCase());
        } else if (isRepairable(result.getType())) {
            // Restrict anvil rewards to durability-bearing items to keep "repair" meaningful.
            exp.awardExp(player, ExpAction.REPAIR_ITEM, result.getType().name().toLowerCase());
        }
    }

    /**
     * Determines whether a material is a repairable (durability-bearing) item.
     * @param type The material.
     * @return true if the item has a durability bar.
     */
    private boolean isRepairable(Material type) {
        return type.getMaxDurability() > 0;
    }
}
