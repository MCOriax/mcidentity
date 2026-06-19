package io.github.mcoriax.bukkit.listeners;

import com.cryptomorin.xseries.XMaterial;
import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles crafting and processing related EXP actions:
 * {@code craft_item}, {@code smelt_furnace} and {@code brew_potion}.
 */
public class CraftingExpListener implements Listener {

    /** Highest result-slot index of a brewing stand inventory. */
    private static final int BREWING_RESULT_SLOT_MAX = 2;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new CraftingExpListener.
     * @param plugin The plugin instance.
     */
    public CraftingExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player crafts an item.
     * @param event The craft item event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player) || event.getRecipe() == null) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        exp.awardExp(player, ExpAction.CRAFT_ITEM, result.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player extracts a smelted item from a furnace.
     * @param event The furnace extract event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || event.getItemType() == null) {
            return;
        }

        exp.awardExp(player, ExpAction.SMELT_FURNACE, event.getItemType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player collects a brewed potion from a brewing stand.
     * @param event The inventory click event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewingCollect(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getType() != InventoryType.BREWING) {
            return;
        }
        // Only the three potion result slots of the brewing stand are relevant.
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot > BREWING_RESULT_SLOT_MAX) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current == null || !isPotion(current.getType())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.BREW_POTION);
    }

    /**
     * Determines whether a material is any kind of potion.
     * @param type The material.
     * @return true if the material is a potion variant.
     */
    private boolean isPotion(Material type) {
        XMaterial xType = XMaterial.matchXMaterial(type);
        return xType == XMaterial.POTION
                || xType == XMaterial.SPLASH_POTION
                || xType == XMaterial.LINGERING_POTION;
    }
}
