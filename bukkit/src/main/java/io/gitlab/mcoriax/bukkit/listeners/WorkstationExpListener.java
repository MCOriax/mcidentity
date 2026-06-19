package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles workstation and economy related EXP actions:
 * {@code sculpt_chiseled_block}, {@code use_smithing_template}, {@code repair_trident},
 * {@code complete_map_fill} and {@code trade_special_wandering}.
 *
 * <p>Every action is detected by the player taking the produced result out of a
 * workstation, which means a genuine recipe was consumed (materials, XP levels or a
 * smithing template). That material cost is the natural anti-exploit gate, so no
 * cooldown is required. The result slot index differs per workstation, so each type is
 * dispatched separately.</p>
 */
public class WorkstationExpListener implements Listener {

    /** Result slot of a stonecutter inventory. */
    private static final int STONECUTTER_RESULT_SLOT = 1;
    /** Result slot shared by anvil, merchant and cartography inventories. */
    private static final int STANDARD_RESULT_SLOT = 2;
    /** Result slot of a smithing table inventory. */
    private static final int SMITHING_RESULT_SLOT = 3;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new WorkstationExpListener.
     * @param plugin The plugin instance.
     */
    public WorkstationExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Routes a workstation result pickup to the matching handler.
     * @param event The inventory click event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Only react to the player actually taking a produced result item.
        if (event.getAction() == InventoryAction.NOTHING) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryType type = top.getType();
        int slot = event.getRawSlot();
        ItemStack result = event.getCurrentItem();

        if (type == InventoryType.ANVIL && slot == STANDARD_RESULT_SLOT) {
            handleRepairTrident(player, result);
        } else if (type == InventoryType.MERCHANT && slot == STANDARD_RESULT_SLOT) {
            handleWanderingTrade(player, top, result);
        } else if (type == InventoryType.CARTOGRAPHY && slot == STANDARD_RESULT_SLOT) {
            handleMapFill(player, top, result);
        } else if (type == InventoryType.STONECUTTER && slot == STONECUTTER_RESULT_SLOT) {
            handleChiseledBlock(player, result);
        } else if (type == InventoryType.SMITHING && slot == SMITHING_RESULT_SLOT) {
            handleSmithingTemplate(player, result);
        }
    }

    /**
     * Awards EXP when the anvil result is a repaired trident.
     * @param player The player taking the result.
     * @param result The produced result item.
     */
    private void handleRepairTrident(Player player, ItemStack result) {
        if (result == null || result.getType() != Material.TRIDENT) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.REPAIR_TRIDENT);
    }

    /**
     * Awards EXP when the trade was taken from a wandering trader specifically.
     * @param player The trading player.
     * @param top The merchant inventory.
     * @param result The produced result item.
     */
    private void handleWanderingTrade(Player player, Inventory top, ItemStack result) {
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        // Villager merchants are handled elsewhere; this action is exclusive to wandering traders.
        if (!(top instanceof MerchantInventory merchantInventory)) {
            return;
        }
        Merchant merchant = merchantInventory.getMerchant();
        if (!(merchant instanceof WanderingTrader)) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.TRADE_SPECIAL_WANDERING, result.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when the player finalizes a map by locking it at a cartography table.
     * @param player The player taking the result.
     * @param top The cartography inventory.
     * @param result The produced result item.
     */
    private void handleMapFill(Player player, Inventory top, ItemStack result) {
        if (result == null || result.getType() != Material.FILLED_MAP) {
            return;
        }
        // Locking is the only cartography recipe that consumes a glass pane, and it captures
        // a permanent, finished snapshot of the explored map (copying/zooming use paper).
        ItemStack ingredient = top.getItem(1);
        if (ingredient == null || ingredient.getType() != Material.GLASS_PANE) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.COMPLETE_MAP_FILL);
    }

    /**
     * Awards EXP when the stonecutter produces a chiseled block variant.
     * @param player The player taking the result.
     * @param result The produced result item.
     */
    private void handleChiseledBlock(Player player, ItemStack result) {
        if (result == null || !result.getType().name().startsWith("CHISELED_")) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.SCULPT_CHISELED_BLOCK, result.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a smithing table result is taken, which always consumes a template.
     * @param player The player taking the result.
     * @param result The produced result item.
     */
    private void handleSmithingTemplate(Player player, ItemStack result) {
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.USE_SMITHING_TEMPLATE, result.getType().name().toLowerCase());
    }
}
