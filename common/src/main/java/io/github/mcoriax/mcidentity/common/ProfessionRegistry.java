package io.github.mcoriax.mcidentity.common;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The in-memory registry of everything the profession files configure.
 *
 * <p><strong>Internal.</strong> Package-private on purpose: callers reach this
 * state exclusively through the matching {@code getProfession*} and
 * {@code setProfession*} methods on {@link MCIdentityManager}, which is the
 * single public entry point for RAM state. Extracted so the manager stays a
 * facade rather than also being the storage for five unrelated maps.</p>
 *
 * <p>Every map is keyed by the lower-cased profession system code and is a
 * {@link ConcurrentHashMap}, because profession configuration is read from
 * gameplay threads while it is (re)loaded on the main thread.</p>
 */
final class ProfessionRegistry {

    /**
     * Level ceiling and per-level EXP requirement, by profession code.
     */
    private static final Map<String, MCIdentityManager.ProfessionLevelConfig> LEVEL_CONFIGS = new ConcurrentHashMap<>();

    /**
     * Raw EXP gain configuration read from each profession file, by profession code.
     */
    private static final Map<String, Map<String, Object>> EXP_CONFIGS = new ConcurrentHashMap<>();

    /**
     * Human readable profession names, by profession code. Doubles as the
     * registry of which professions exist at all.
     */
    private static final Map<String, String> DISPLAY_NAMES = new ConcurrentHashMap<>();

    /**
     * Starting stat values handed to a new identity, by profession code.
     */
    private static final Map<String, Map<String, Integer>> BASE_STATS = new ConcurrentHashMap<>();

    /**
     * Stat randomization expression applied on identity creation, by profession code.
     */
    private static final Map<String, String> STATS_RANDOM = new ConcurrentHashMap<>();

    /**
     * Level-keyed skull textures, by profession code.
     */
    private static final Map<String, Map<Integer, String>> SKINS = new ConcurrentHashMap<>();

    /**
     * Prevents instantiation of this state holder.
     */
    private ProfessionRegistry() {
    }

    /**
     * Normalises a profession code to the casing the maps are keyed by.
     * @param professionCode The profession system code.
     * @return The lower-cased code.
     */
    private static String key(String professionCode) {
        return professionCode.toLowerCase(Locale.ROOT);
    }

    /**
     * Stores the level configuration for a profession.
     * @param professionCode The system code of the profession.
     * @param maxLevel The maximum level.
     * @param expUp The EXP required for next level.
     */
    static void setLevelConfig(String professionCode, int maxLevel, int expUp) {
        LEVEL_CONFIGS.put(key(professionCode), new MCIdentityManager.ProfessionLevelConfig(maxLevel, expUp));
    }

    /**
     * Gets the level configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The level configuration, or a permissive default when unset.
     */
    static MCIdentityManager.ProfessionLevelConfig getLevelConfig(String professionCode) {
        return LEVEL_CONFIGS.getOrDefault(key(professionCode),
            new MCIdentityManager.ProfessionLevelConfig(100, 10000));
    }

    /**
     * Stores the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @param config The EXP configuration map.
     */
    static void setExpConfig(String professionCode, Map<String, Object> config) {
        EXP_CONFIGS.put(key(professionCode), config);
    }

    /**
     * Gets the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The EXP configuration map, empty when unset.
     */
    static Map<String, Object> getExpConfig(String professionCode) {
        return EXP_CONFIGS.getOrDefault(key(professionCode), Collections.emptyMap());
    }

    /**
     * Stores the display name for a profession, which also registers it.
     * @param professionCode The system code of the profession.
     * @param displayName The display name.
     */
    static void setDisplayName(String professionCode, String displayName) {
        DISPLAY_NAMES.put(key(professionCode), displayName);
    }

    /**
     * Gets the display name for a profession.
     * @param professionCode The system code of the profession.
     * @return The display name, falling back to the code itself.
     */
    static String getDisplayName(String professionCode) {
        return DISPLAY_NAMES.getOrDefault(key(professionCode), professionCode);
    }

    /**
     * Gets every registered profession code with its display name.
     * @return An unmodifiable view of the registry.
     */
    static Map<String, String> getRegistered() {
        return Collections.unmodifiableMap(DISPLAY_NAMES);
    }

    /**
     * Stores the base stats for a profession.
     * @param professionCode The system code of the profession.
     * @param stats The base stats map.
     */
    static void setBaseStats(String professionCode, Map<String, Integer> stats) {
        BASE_STATS.put(key(professionCode), stats);
    }

    /**
     * Gets the base stats for a profession.
     * @param professionCode The system code of the profession.
     * @return The base stats map, empty when unset.
     */
    static Map<String, Integer> getBaseStats(String professionCode) {
        return BASE_STATS.getOrDefault(key(professionCode), Collections.emptyMap());
    }

    /**
     * Stores the stats randomization for a profession.
     * @param professionCode The system code of the profession.
     * @param random The randomization config string.
     */
    static void setStatsRandom(String professionCode, String random) {
        STATS_RANDOM.put(key(professionCode), random);
    }

    /**
     * Gets the stats randomization for a profession.
     * @param professionCode The system code of the profession.
     * @return The randomization config string, "0" when unset.
     */
    static String getStatsRandom(String professionCode) {
        return STATS_RANDOM.getOrDefault(key(professionCode), "0");
    }

    /**
     * Stores the skins for a profession.
     * @param professionCode The system code of the profession.
     * @param skins The skins map.
     */
    static void setSkins(String professionCode, Map<Integer, String> skins) {
        SKINS.put(key(professionCode), skins);
    }

    /**
     * Gets the skins for a profession.
     * @param professionCode The system code of the profession.
     * @return The skins map, empty when unset.
     */
    static Map<Integer, String> getSkins(String professionCode) {
        return SKINS.getOrDefault(key(professionCode), Collections.emptyMap());
    }
}
