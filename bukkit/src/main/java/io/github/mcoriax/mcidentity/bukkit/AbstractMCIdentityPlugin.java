package io.github.mcoriax.mcidentity.bukkit;

import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.bukkit.bridge.mcagents.MCAgentsToolRegistrar;
import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.listeners.AdvancedExplorationExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.BuildingAndSurvivalExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.CombatExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.CraftingExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.DimensionsExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.EngineeringExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.ExplorationAndTravelExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.FarmingAndRanchingExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.GatheringExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.IdentityAliasNameListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.InteractionAndArtExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.InteractionExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityInventoryListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.PlayerLoginListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.PlayerRespawnListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MagicAndEconomyExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SpecialEventsExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SpecialtyFarmingExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SupportExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SurvivalExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.UtilityAndCombatExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.RanchingAndWildlifeExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.WorkstationExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.WorldInteractionExpListener;
import io.github.mcoriax.mcidentity.bukkit.tabcompleter.MCIdentityTabCompleter;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformScheduler;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import io.github.mcoriax.mcidentity.bukkit.util.PassiveExpService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformTask;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared bootstrap for every platform plugin entry point.
 * <p>
 * The enable/disable lifecycle, config loading, profession parsing, command and
 * listener registration, and database wiring are identical across SpigotMC,
 * PaperMC, and Folia. Each platform module supplies only its scheduler via
 * {@link #createScheduler()} and extends this class with a concrete entry point.
 * </p>
 */
public abstract class AbstractMCIdentityPlugin extends JavaPlugin {

    /**
     * Milliseconds between storage initialization retries.
     */
    private static final long STORAGE_RETRY_PERIOD_MILLIS = 10_000L;

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
     * Whether the schema initialization and profession registration chain has
     * completed successfully since enable.
     */
    private volatile boolean storageInitialized;

    /**
     * Guards against overlapping initialization attempts when one attempt is
     * still waiting on the database while the retry ticker fires again.
     */
    private final AtomicBoolean storageInitAttemptInFlight = new AtomicBoolean();

    /**
     * The 10-second retry ticker driving initialization attempts until one
     * succeeds; null once cancelled.
     */
    private volatile PlatformTask storageRetryTask;

    /**
     * Supplies the platform specific scheduler installed during {@link #onEnable()}.
     * @return the scheduler implementation for this platform.
     */
    protected abstract PlatformScheduler createScheduler();

    /**
     * Whether the storage backend is fully initialized: the schema exists and
     * the loaded professions are registered. The central-server login
     * protection refuses players until this is true.
     * @return True once storage initialization has completed successfully.
     */
    public boolean isStorageInitialized() {
        return storageInitialized;
    }

    @Override
    /**
     * Boots the plugin: loads configuration and professions, brings storage up
     * through {@link MCIdentityProvider}, and registers commands and listeners.
     */
    public void onEnable() {
        Schedulers.set(createScheduler());

        // Only config.yml is written up front, because the enable flag lives in
        // it and the admin needs the file to toggle the plugin on. Everything
        // else (textures.yml, the professions folder, and the SQLite
        // mcidentity.db created later in setupDatabase) is generated only once
        // the plugin is enabled, so a disabled plugin never litters the data
        // folder.
        saveDefaultConfig();

        if (!getConfig().getBoolean("enable", false)) {
            getLogger().warning("Plugin is disabled in config.yml. No data files were generated. Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig("textures", false);
        loadCustomConfigs();
        registerBaseProfession();

        checkLicense(getConfig().getString("license", "free"));

        MCIdentityManager.setIdentitySwapCooldownMinutes(getConfig().getInt("identity_swap_cooldown", 0));
        MCIdentityManager.setExperienceManager(new ExperienceManager(this));

        setupDatabase();
        PassiveExpService.start(this);

        getCommand("identity").setExecutor(new MCIdentityCommand(this));
        getCommand("identity").setTabCompleter(new MCIdentityTabCompleter());

        getServer().getPluginManager().registerEvents(new MCIdentityListener(this), this);
        getServer().getPluginManager().registerEvents(new MCIdentityInventoryListener(this), this);
        MCIdentityManager.setStatsListener(new MCIdentityStatsListener(this));
        getServer().getPluginManager().registerEvents((MCIdentityStatsListener) MCIdentityManager.getStatsListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new IdentityAliasNameListener(this), this);

        // Modular EXP gain listeners grouped by category.
        getServer().getPluginManager().registerEvents(new CombatExpListener(this), this);
        getServer().getPluginManager().registerEvents(new GatheringExpListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingExpListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionExpListener(this), this);
        getServer().getPluginManager().registerEvents(new MagicAndEconomyExpListener(this), this);
        getServer().getPluginManager().registerEvents(new BuildingAndSurvivalExpListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancedExplorationExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SupportExpListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingAndRanchingExpListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplorationAndTravelExpListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionAndArtExpListener(this), this);
        getServer().getPluginManager().registerEvents(new UtilityAndCombatExpListener(this), this);
        getServer().getPluginManager().registerEvents(new EngineeringExpListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionsExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SurvivalExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialEventsExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialtyFarmingExpListener(this), this);
        getServer().getPluginManager().registerEvents(new RanchingAndWildlifeExpListener(this), this);
        getServer().getPluginManager().registerEvents(new WorkstationExpListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldInteractionExpListener(this), this);

        // Optional: expose the read-only identity tools to MCAgents when it is
        // installed. Reflective, so MCIdentity runs unchanged without it.
        if (isEnabled()) {
            MCAgentsToolRegistrar.registerAll(this);
        }

        getLogger().info("MCIdentityPlugin has been enabled!");
    }

    /**
     * Verifies the license type and logs information.
     * @param license The license key/type.
     */
    private void checkLicense(String license) {
        if (license.equalsIgnoreCase("free")) {
            getLogger().info("Running with free license. Some features may be limited.");
        } else if (license.equalsIgnoreCase("premium")) {
            getLogger().info("Running with premium license. All features enabled!");
        } else {
            getLogger().warning("Unknown license type in config.yml. Defaulting to free license.");
        }
    }

    /**
     * Registers the built-in "player" base profession. Its configuration is
     * hardcoded — unlimited levels at a fixed EXP cost per level, no action
     * based EXP — and always overrides any profession file using its code.
     */
    private void registerBaseProfession() {
        MCIdentityManager.setProfessionDisplayName(BaseProfession.CODE, BaseProfession.DISPLAY_NAME);
        MCIdentityManager.setProfessionLevelConfig(BaseProfession.CODE, BaseProfession.MAX_LEVEL, BaseProfession.EXP_PER_LEVEL);
        MCIdentityManager.setProfessionStatsRandom(BaseProfession.CODE, "0");
        MCIdentityManager.setProfessionBaseStats(BaseProfession.CODE, Map.of());
        MCIdentityManager.setProfessionExpConfig(BaseProfession.CODE, Map.of());
    }

    /**
     * Loads custom configuration files.
     */
    private void loadCustomConfigs() {
        File texturesFile = new File(getDataFolder(), "textures.yml");
        MCIdentityManager.setTexturesConfig(YamlConfiguration.loadConfiguration(texturesFile));

        loadProfessions();
    }

    /**
     * Loads all profession configurations from the professions folder.
     */
    private void loadProfessions() {
        File professionsFolder = new File(getDataFolder(), "professions");
        if (!professionsFolder.exists()) {
            professionsFolder.mkdirs();
            // Save default professions if they don't exist
            for (String profession : DEFAULT_PROFESSIONS) {
                saveResource("professions/" + profession + ".yml", false);
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
        getLogger().info("Loaded profession config for: " + displayName + " (" + code + ")");
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
    private Map<String, Object> readExpConfig(FileConfiguration config) {
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
    private Map<String, Object> convertSectionToMap(ConfigurationSection section) {
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

    /**
     * Saves a default configuration file if it doesn't exist.
     * @param name The name of the resource.
     * @param replace Whether to replace the existing file.
     */
    public void saveDefaultConfig(String name, boolean replace) {
        File file = new File(getDataFolder(), name + ".yml");
        if (!file.exists() || replace) {
            saveResource(name + ".yml", replace);
        }
    }

    /**
     * Gets the experience manager instance.
     * @return The experience manager.
     */
    public ExperienceManager getExperienceManager() {
        return (ExperienceManager) MCIdentityManager.getExperienceManager();
    }

    /**
     * Gets the identity stats listener instance.
     * @return The stats listener.
     */
    public MCIdentityStatsListener getStatsListener() {
        return (MCIdentityStatsListener) MCIdentityManager.getStatsListener();
    }

    /**
     * Gets the textures configuration instance.
     * @return The textures configuration.
     */
    public FileConfiguration getTexturesConfig() {
        return (FileConfiguration) MCIdentityManager.getTexturesConfig();
    }

    /**
     * Initializes the storage backend selected by {@code database.type} in
     * config.yml — SQLite (the default), MySQL, PostgreSQL, MongoDB, or the
     * MCIdentity central server — and registers the loaded professions there.
     */
    private void setupDatabase() {
        String type = getConfig().getString("database.type", "sqlite");

        MCIdentityProvider provider;
        try {
            provider = MCIdentityProvider.create(type, databaseOptions(type));
        } catch (IllegalArgumentException e) {
            getLogger().severe(e.getMessage() + " (supported: sqlite, mysql, postgresql, mongodb, central). Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Cold-boot login protection: with remote storage, hold logins until
        // initialization has completed and the (possibly cold-started) server
        // answers its health probe, so players never join while their data is
        // unreachable. Which backend that is stays behind the provider.
        if (provider.isRemoteStorage()) {
            getServer().getPluginManager().registerEvents(
                new PlayerLoginListener(this, this::isStorageInitialized), this);
        }

        // The ticker keeps retrying the initialization until one attempt
        // succeeds, so a cold-started central server (or a briefly unreachable
        // database) only delays startup instead of leaving the plugin
        // half-initialized. The first attempt runs immediately.
        storageRetryTask = Schedulers.runAsyncAtFixedRate(
            () -> attemptStorageInitialization(provider, type),
            STORAGE_RETRY_PERIOD_MILLIS, STORAGE_RETRY_PERIOD_MILLIS);
        attemptStorageInitialization(provider, type);
    }

    /**
     * Runs one attempt of the schema initialization and profession
     * registration chain. Attempts are skipped while storage is already
     * initialized or a previous attempt is still in flight; a failed attempt
     * is logged and retried by the ticker every ten seconds.
     * @param provider The provider whose storage should be initialized.
     * @param type The configured database type, used in log messages.
     */
    private void attemptStorageInitialization(MCIdentityProvider provider, String type) {
        if (storageInitialized || !storageInitAttemptInFlight.compareAndSet(false, true)) {
            return;
        }
        provider.initializeStorage()
            .thenCompose(v -> registerProfessionsSequentially())
            .whenComplete((v, ex) -> {
                storageInitAttemptInFlight.set(false);
                if (ex == null) {
                    storageInitialized = true;
                    cancelStorageRetryTask();
                    getLogger().info("Connected to " + type + " storage and registered professions");
                } else {
                    getLogger().warning("Failed to initialize the " + type + " storage: " + ex.getMessage()
                        + ". Retrying in 10 seconds...");
                }
            });
    }

    /**
     * Cancels the storage initialization retry ticker if it is still armed.
     */
    private void cancelStorageRetryTask() {
        PlatformTask task = storageRetryTask;
        if (task != null) {
            task.cancel();
            storageRetryTask = null;
        }
    }

    /**
     * Collects the connection settings of the selected database type from
     * config.yml.
     * @param type The configured database type.
     * @return The connection settings passed to the backend factory.
     */
    private Map<String, Object> databaseOptions(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        String sectionPath = switch (normalized) {
            case "central", "central_server" -> "central_server";
            case "postgres" -> "database.postgresql";
            case "mongo" -> "database.mongodb";
            case "mariadb" -> "database.mysql";
            default -> "database." + normalized;
        };

        ConfigurationSection section = getConfig().getConfigurationSection(sectionPath);
        Map<String, Object> options = section != null ? new HashMap<>(section.getValues(false)) : new HashMap<>();

        if (normalized.equals("sqlite")) {
            // The configured file name is relative to the plugin data folder.
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            String fileName = options.getOrDefault("file", "mcidentity.db").toString();
            options.put("file", new File(getDataFolder(), fileName).getAbsolutePath());
        }
        return options;
    }

    /**
     * Registers all loaded professions in the database sequentially.
     * @return A CompletableFuture representing the registration process.
     */
    private CompletableFuture<Void> registerProfessionsSequentially() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (Map.Entry<String, String> entry : MCIdentityManager.getRegisteredProfessions().entrySet()) {
            future = future.thenCompose(v -> MCIdentityProvider.instance.createProfession(entry.getKey(), entry.getValue()));
        }
        return future;
    }

    @Override
    /**
     * Shuts the plugin down: flushes buffered EXP, cancels the storage retry
     * ticker, and closes the storage backend through the provider.
     */
    public void onDisable() {
        cancelStorageRetryTask();

        // Flush buffered passive EXP before the database client goes away.
        PassiveExpService.stop();

        if (MCIdentityManager.getStatsListener() != null) {
            ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).cancelTasks();
        }

        // Gracefully close the connection pool
        if (MCIdentityProvider.hasDatabase()) {
            MCIdentityProvider.instance.shutdown();
        }

        getLogger().info("MCIdentityPlugin has been disabled!");
    }
}
