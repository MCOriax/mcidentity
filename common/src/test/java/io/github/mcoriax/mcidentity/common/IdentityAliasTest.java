package io.github.mcoriax.mcidentity.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the shared alias rule every backend and command path validates
 * against.
 */
class IdentityAliasTest {

    @Test
    @DisplayName("Alphanumeric values of an allowed length are accepted")
    void acceptsAlphanumeric() {
        assertTrue(IdentityAlias.isValid("Alex"));
        assertTrue(IdentityAlias.isValid("alex"));
        assertTrue(IdentityAlias.isValid("Alex99"));
        assertTrue(IdentityAlias.isValid("0"));
        assertTrue(IdentityAlias.isValid("a".repeat(IdentityAlias.MAX_LENGTH)));
    }

    @Test
    @DisplayName("Anything but letters and digits is rejected")
    void rejectsNonAlphanumeric() {
        assertFalse(IdentityAlias.isValid(null));
        assertFalse(IdentityAlias.isValid(""));
        assertFalse(IdentityAlias.isValid("has space"));
        assertFalse(IdentityAlias.isValid("under_score"));
        assertFalse(IdentityAlias.isValid("dash-ed"));
        assertFalse(IdentityAlias.isValid("emoji😀"));
        assertFalse(IdentityAlias.isValid("dröm"));
        assertFalse(IdentityAlias.isValid("' OR 1=1 --"));
        assertFalse(IdentityAlias.isValid("a".repeat(IdentityAlias.MAX_LENGTH + 1)));
    }

    @Test
    @DisplayName("The comparison form folds case so equal names collide")
    void comparableFoldsCase() {
        assertEquals("alex", IdentityAlias.comparable("Alex"));
        assertEquals("alex", IdentityAlias.comparable("ALEX"));
        assertEquals(IdentityAlias.comparable("Alex"), IdentityAlias.comparable("aLeX"));
        assertNull(IdentityAlias.comparable(null));
    }
}
