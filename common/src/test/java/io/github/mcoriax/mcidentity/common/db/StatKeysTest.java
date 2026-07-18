package io.github.mcoriax.mcidentity.common.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link StatKeys} allow-list that guards stat identifiers used as
 * SQL column names.
 */
class StatKeysTest {

    @Test
    @DisplayName("All known stat identifiers are accepted")
    void testValidKeys() {
        for (String key : new String[] {"energy", "health", "food", "strength",
                "defense", "wisdom", "luck", "agility", "resilience"}) {
            assertTrue(StatKeys.isValid(key), key + " should be valid");
        }
    }

    @Test
    @DisplayName("Null, unknown and injection-style keys are rejected")
    void testInvalidKeys() {
        assertFalse(StatKeys.isValid(null));
        assertFalse(StatKeys.isValid(""));
        assertFalse(StatKeys.isValid("invalid_stat"));
        assertFalse(StatKeys.isValid("energy; DROP TABLE identity_stats"));
        assertFalse(StatKeys.isValid("energy --"));
    }

    @Test
    @DisplayName("Validation is case-sensitive to match the stored column names")
    void testCaseSensitive() {
        assertFalse(StatKeys.isValid("ENERGY"));
        assertFalse(StatKeys.isValid("Health"));
    }
}
