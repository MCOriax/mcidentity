package io.gitlab.mcoriax.bukkit.util;

/**
 * Central registry of EXP gain action keys.
 *
 * <p>Each constant maps directly to an action key that server owners can define
 * inside a profession's YAML configuration. Centralizing them here avoids magic
 * strings scattered across the listeners and keeps the configuration contract in
 * a single, documented place.</p>
 */
public final class ExpAction {

    private ExpAction() {
        // Utility class; not meant to be instantiated.
    }

    // --- Core actions (pre-existing) ---
    /** Triggered when the player takes damage. */
    public static final String RECEIVE_DAMAGE = "receive_damage";
    /** Triggered when the player deals melee damage to another entity. */
    public static final String DEAL_DAMAGE = "deal_damage";
    /** Triggered when the player kills an entity. */
    public static final String KILL_MOB = "kill_mob";

    // --- Combat & Defense ---
    /** Triggered when the player deals damage with a projectile (e.g. a bow). */
    public static final String DEAL_RANGED_DAMAGE = "deal_ranged_damage";
    /** Triggered when the player blocks incoming damage with a shield. */
    public static final String SHIELD_BLOCK = "shield_block";
    /** Triggered when the player lands a critical melee hit. */
    public static final String CRITICAL_HIT = "critical_hit";
    /** Triggered when the player evades incoming damage (agility based). */
    public static final String DODGE_ACTION = "dodge_action";

    // --- Gathering ---
    /** Triggered when the player breaks an ore block. */
    public static final String BREAK_ORE = "break_ore";
    /** Triggered when the player breaks a log block. */
    public static final String CHOP_TREE = "chop_tree";
    /** Triggered when the player harvests a fully grown crop. */
    public static final String HARVEST_CROP = "harvest_crop";
    /** Triggered when the player reels in a fish. */
    public static final String FISH_CAUGHT = "fish_caught";

    // --- Crafting & Processing ---
    /** Triggered when the player crafts an item. */
    public static final String CRAFT_ITEM = "craft_item";
    /** Triggered when the player extracts a smelted item from a furnace. */
    public static final String SMELT_FURNACE = "smelt_furnace";
    /** Triggered when the player collects a brewed potion. */
    public static final String BREW_POTION = "brew_potion";

    // --- Interaction & Exploration ---
    /** Triggered when the player tames an animal. */
    public static final String TAME_ANIMAL = "tame_animal";
    /** Triggered when the player breeds two animals. */
    public static final String BREED_ANIMAL = "breed_animal";
    /** Triggered when the player enters a biome for the first time this session. */
    public static final String EXPLORE_BIOME = "explore_biome";
    /** Triggered when the player heals another entity. */
    public static final String HEAL_TARGET = "heal_target";
}
