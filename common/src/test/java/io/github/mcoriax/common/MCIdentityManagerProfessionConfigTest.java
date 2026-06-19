package io.github.mcoriax.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the in-memory profession configuration registry on {@link MCIdentityManager}.
 */
class MCIdentityManagerProfessionConfigTest {

    @Test
    @DisplayName("Test profession configuration storage")
    void testProfessionConfigs() {
        String profCode = "warrior";
        String displayName = "Warrior Class";

        MCIdentityManager.setProfessionDisplayName(profCode, displayName);
        assertEquals(displayName, MCIdentityManager.getProfessionDisplayName(profCode));
        assertEquals(displayName, MCIdentityManager.getProfessionDisplayName(profCode.toUpperCase()));

        MCIdentityManager.setProfessionLevelConfig(profCode, 50, 5000);
        MCIdentityManager.ProfessionLevelConfig config = MCIdentityManager.getProfessionLevelConfig(profCode);
        assertEquals(50, config.maxLevel());
        assertEquals(5000, config.expUp());
    }

    @Test
    @DisplayName("Test profession metadata (base stats, randomization, skins, exp) storage")
    void testProfessionMetadata() {
        String code = "rogue_meta";

        Map<String, Integer> baseStats = new HashMap<>();
        baseStats.put("strength", 7);
        MCIdentityManager.setProfessionBaseStats(code, baseStats);
        assertEquals(7, MCIdentityManager.getProfessionBaseStats(code).get("strength"));
        // Lookups are case-insensitive on the profession code.
        assertEquals(7, MCIdentityManager.getProfessionBaseStats(code.toUpperCase()).get("strength"));

        MCIdentityManager.setProfessionStatsRandom(code, "5");
        assertEquals("5", MCIdentityManager.getProfessionStatsRandom(code));
        // Defaults to "0" when never configured.
        assertEquals("0", MCIdentityManager.getProfessionStatsRandom("never_configured_profession"));

        Map<Integer, String> skins = new HashMap<>();
        skins.put(10, "skin-at-10");
        MCIdentityManager.setProfessionSkins(code, skins);
        assertEquals("skin-at-10", MCIdentityManager.getProfessionSkins(code).get(10));

        Map<String, Object> expConfig = new HashMap<>();
        expConfig.put("source", "mining");
        MCIdentityManager.setProfessionExpConfig(code, expConfig);
        assertEquals("mining", MCIdentityManager.getProfessionExpConfig(code).get("source"));
        // Defaults to an empty map when never configured.
        assertTrue(MCIdentityManager.getProfessionExpConfig("never_configured_profession").isEmpty());

        MCIdentityManager.setProfessionDisplayName(code, "Rogue Meta");
        assertTrue(MCIdentityManager.getRegisteredProfessions().containsKey(code));
        assertEquals("Rogue Meta", MCIdentityManager.getRegisteredProfessions().get(code));
    }
}
