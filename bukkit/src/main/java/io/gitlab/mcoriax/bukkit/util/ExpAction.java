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

    // --- Magic & Economy ---
    /** Triggered when the player enchants an item at an enchanting table. */
    public static final String ENCHANT_ITEM = "enchant_item";
    /** Triggered when the player completes a trade with a villager. */
    public static final String VILLAGER_TRADE = "villager_trade";
    /** Triggered when the player repairs or combines an item on an anvil. */
    public static final String REPAIR_ITEM = "repair_item";

    // --- Building & Survival ---
    /** Triggered when the player places a block (anti-exploit guarded). */
    public static final String PLACE_BLOCK = "place_block";
    /** Triggered when the player consumes a food item. */
    public static final String CONSUME_FOOD = "consume_food";
    /** Triggered when the player shears an entity (e.g. a sheep). */
    public static final String SHEAR_ENTITY = "shear_entity";

    // --- Advanced Exploration & Movement ---
    /** Triggered when the player opens a naturally generated loot container. */
    public static final String OPEN_LOOT_CHEST = "open_loot_chest";
    /** Triggered when the player starts gliding with an elytra. */
    public static final String GLIDE_ELYTRA = "glide_elytra";
    /** Triggered when the player survives fall damage. */
    public static final String SURVIVE_FALL = "survive_fall";

    // --- Team & Support ---
    /** Triggered when the player applies a beneficial effect to another player. */
    public static final String APPLY_BUFF = "apply_buff";

    // --- Farming & Ranching ---
    /** Triggered when the player plants a crop seed. */
    public static final String PLANT_CROP = "plant_crop";
    /** Triggered when the player strips a log with an axe. */
    public static final String STRIP_LOG = "strip_log";
    /** Triggered when the player milks an entity with a bucket. */
    public static final String MILK_ENTITY = "milk_entity";
    /** Triggered when the player captures an entity inside a bucket (e.g. a fish). */
    public static final String CATCH_ENTITY_BUCKET = "catch_entity_bucket";

    // --- Exploration & Travel ---
    /** Triggered when the player discovers a structure (detected via its advancement). */
    public static final String DISCOVER_STRUCTURE = "discover_structure";
    /** Triggered when the player starts riding a vehicle (boat, minecart). */
    public static final String RIDE_VEHICLE = "ride_vehicle";
    /** Triggered when the player leashes an entity. */
    public static final String LEASH_ENTITY = "leash_entity";

    // --- Interaction & Art ---
    /** Triggered when the player plays a music disc in a jukebox. */
    public static final String PLAY_MUSIC_DISC = "play_music_disc";
    /** Triggered when the player dyes a dyeable entity. */
    public static final String DYE_ITEM = "dye_item";
    /** Triggered when the player carves a pumpkin with shears. */
    public static final String CARVE_PUMPKIN = "carve_pumpkin";
    /** Triggered when the player rings a bell. */
    public static final String RING_BELL = "ring_bell";

    // --- Magic, Utility & Combat ---
    /** Triggered when the player ignites a fire (e.g. with flint and steel). */
    public static final String IGNITE_FIRE = "ignite_fire";
    /** Triggered when the player drinks a potion. */
    public static final String CONSUME_POTION = "consume_potion";
    /** Triggered when the player applies a harmful effect to another entity. */
    public static final String APPLY_DEBUFF = "apply_debuff";
    /** Triggered when the player cures a zombie villager. */
    public static final String CURE_ZOMBIE_VILLAGER = "cure_zombie_villager";

    // --- Engineering & Mechanics ---
    /** Triggered when the player places a redstone component (keyed by block material). */
    public static final String PLACE_REDSTONE = "place_redstone";
    /** Triggered when a player builds a utility golem such as an iron or snow golem (keyed by entity type). */
    public static final String CREATE_GOLEM = "create_golem";
    /** Triggered when the player places (activates) a conduit. */
    public static final String ACTIVATE_CONDUIT = "activate_conduit";

    // --- Dimensions & Aquatic ---
    /** Triggered for every fixed segment of distance the player swims. */
    public static final String SWIM_DISTANCE = "swim_distance";
    /** Triggered when the player travels through a nether or end portal (keyed by destination environment). */
    public static final String ENTER_PORTAL = "enter_portal";
    /** Triggered when a totem of undying saves the player from death. */
    public static final String USE_TOTEM = "use_totem";

    // --- Survival & Resilience ---
    /** Triggered when the player successfully lies down to sleep in a bed. */
    public static final String SLEEP_IN_BED = "sleep_in_bed";
    /** Triggered when the player extinguishes a fire block (keyed by fire material). */
    public static final String EXTINGUISH_FIRE = "extinguish_fire";
    /** Triggered when the player takes explosion damage but survives it (numerical, keyed by the survived damage amount). */
    public static final String SURVIVE_EXPLOSION = "survive_explosion";

    // --- Special Events & Bosses ---
    /** Triggered when the player triggers a village raid (e.g. by entering a village with Bad Omen). */
    public static final String TRIGGER_RAID = "trigger_raid";
    /** Triggered when the player is among the winners of a village raid. */
    public static final String WIN_RAID = "win_raid";
    /** Triggered when a player summons a boss such as the Wither (keyed by entity type). */
    public static final String SUMMON_BOSS = "summon_boss";

    // --- Specialty Farming & Pets ---
    /** Triggered when the player feeds a tamed pet (keyed by entity type). */
    public static final String FEED_PET = "feed_pet";
    /** Triggered when a thrown egg hatches into a chick. */
    public static final String HATCH_EGG = "hatch_egg";
    /** Triggered when the player collects honey from a full beehive or bee nest with a bottle. */
    public static final String COLLECT_HONEY = "collect_honey";
}
