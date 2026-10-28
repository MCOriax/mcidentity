package io.github.mcoriax.mcidentity.bukkit;

import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads every profession into {@link MCIdentityManager}: the built-in base
 * profession, and one YAML file per profession in the data folder.
 *
 * <p><strong>Internal.</strong> Package-private, run once by
 * {@link AbstractMCIdentityPlugin} on enable.</p>
 */
final class ProfessionConfigLoader {

    /**
     * Config section holding a profession's starting stats.
     */
    private static final String STATS_SECTION = "stats";

    /**
     * Profession fields the plugin reads itself; every other top-level key in a
     * profession file configures an EXP action.
     */
    private static final Set<String> RESERVED_PROFESSION_KEYS = Set.of(
        "system_code", "display_name", "level", STATS_SECTION, "stats_random", "skins");

    /**
     * Professions written into the data folder on first start.
     */
    private static final String[] DEFAULT_PROFESSIONS = {
        "archer", "builder", "crafter", "enchanter", "explorer", "farmer", "fighter",
        "fisher", "gatherer", "supporter", "tamer", "tanker", "acrobat", "archaeologist",
        "chef", "engineer", "guard", "lumberjack", "mariner", "merchant", "miner"
    };

    /**
     * The plugin whose data folder holds the profession files.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a loader.
     * @param plugin The plugin whose data folder holds the profession files.
     */
    ProfessionConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the built-in "player" base profession. Its configuration is
     * hardcoded — unlimited levels at a fixed EXP cost per level, no action
     * based EXP — and always overrides any profession file using its code.
     */
    static void registerBaseProfession() {
        MCIdentityManager.setProfessionDisplayName(BaseProfession.CODE, BaseProfession.DISPLAY_NAME);
        MCIdentityManager.setProfessionLevelConfig(BaseProfession.CODE, BaseProfession.MAX_LEVEL, BaseProfession.EXP_PER_LEVEL);
        MCIdentityManager.setProfessionStatsRandom(BaseProfession.CODE, "0");
        MCIdentityManager.setProfessionBaseStats(BaseProfession.CODE, Map.of());
        MCIdentityManager.setProfessionExpConfig(BaseProfession.CODE, Map.of());
    }

    /**
     * Loads all profession configurations from the professions folder,
     * writing the defaults first when the folder does not exist yet.
     */
    void loadProfessions() {
        File professionsFolder = new File(plugin.getDataFolder(), "professions");
        if (!professionsFolder.exists()) {
            professionsFolder.mkdirs();
            // Save default professions if they don't exist
            for (String profession : DEFAULT_PROFESSIONS) {
                plugin.saveResource("professions/" + profession + ".yml", false);
            }
        }

        File[] files = professionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            loadProfession(file);
        }
    }

    /**
     * Registers one profession from its own YAML file.
     * @param file The profession file.
     */
    private void loadProfession(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String code = professionCode(config, file);
        String displayName = professionDisplayName(config, code);

        // Register in the common manager
        MCIdentityManager.setProfessionDisplayName(code, displayName);

        int maxLevel = config.getInt("level.max", 100);
        int expUp = config.getInt("level.exp_up", 10000);
        MCIdentityManager.setProfessionLevelConfig(code, maxLevel, expUp);

        MCIdentityManager.setProfessionStatsRandom(code, config.getString("stats_random", "0"));
        MCIdentityManager.setProfessionBaseStats(code, readBaseStats(config));

        Map<Integer, String> skins = readSkins(config);
        if (!skins.isEmpty()) {
            MCIdentityManager.setProfessionSkins(code, skins);
        }

        MCIdentityManager.setProfessionExpConfig(code, readExpConfig(config));
        plugin.getLogger().info("Loaded profession config for: " + displayName + " (" + code + ")");
    }

    /**
     * Reads a profession's system code, falling back to its file name.
     * @param config The profession configuration.
     * @param file The file it was read from.
     * @return The lower-cased system code.
     */
    private static String professionCode(FileConfiguration config, File file) {
        String code = config.getString("system_code");
        if (code == null) {
            return file.getName().replace(".yml", "").toLowerCase();
        }
        return code.toLowerCase();
    }

    /**
     * Reads a profession's display name, falling back to its capitalized code.
     * @param config The profession configuration.
     * @param code The profession's system code.
     * @return The display name.
     */
    private static String professionDisplayName(FileConfiguration config, String code) {
        String displayName = config.getString("display_name");
        if (displayName != null) {
            return displayName;
        }
        return code.substring(0, 1).toUpperCase() + code.substring(1);
    }

    /**
     * Reads a profession's configured starting stats.
     * @param config The profession configuration.
     * @return The base stats, empty when the section is absent.
     */
    private static Map<String, Integer> readBaseStats(FileConfiguration config) {
        Map<String, Integer> baseStats = new HashMap<>();
        ConfigurationSection statsSection = config.getConfigurationSection(STATS_SECTION);
        if (statsSection == null) {
            return baseStats;
        }
        for (String statKey : statsSection.getKeys(false)) {
            baseStats.put(statKey, statsSection.getInt(statKey));
        }
        return baseStats;
    }

    /**
     * Reads a profession's skin list.
     * @param config The profession configuration.
     * @return The skins keyed from 1, empty when none are configured.
     */
    private static Map<Integer, String> readSkins(FileConfiguration config) {
        Map<Integer, String> skins = new HashMap<>();
        List<String> skinList = config.getStringList("skins");
        for (int i = 0; i < skinList.size(); i++) {
            // Index entries from 1 to preserve the previous level based keys
            skins.put(i + 1, skinList.get(i));
        }
        return skins;
    }

    /**
     * Reads every remaining key as an EXP action, since anything that is not a
     * reserved profession field configures how that profession earns EXP.
     * @param config The profession configuration.
     * @return The EXP configuration.
     */
    private static Map<String, Object> readExpConfig(FileConfiguration config) {
        Map<String, Object> expConfig = new HashMap<>();
        for (String key : config.getKeys(false)) {
            if (RESERVED_PROFESSION_KEYS.contains(key)) {
                continue;
            }
            if (config.isConfigurationSection(key)) {
                expConfig.put(key, convertSectionToMap(config.getConfigurationSection(key)));
            } else {
                expConfig.put(key, config.getString(key));
            }
        }
        return expConfig;
    }

    /**
     * Converts a configuration section to a Map.
     * @param section The configuration section.
     * @return A map containing the section's keys and values.
     */
    private static Map<String, Object> convertSectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                map.put(key, convertSectionToMap(section.getConfigurationSection(key)));
            } else {
                map.put(key, section.get(key));
            }
        }
        return map;
    }
}
