package io.github.mcoriax.mcidentity.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BaseProfession}, the profession every player wears when they
 * wear nothing else.
 */
class BaseProfessionTest {

    @Test
    @DisplayName("The base profession is recognised by its code, ignoring case")
    void recognisesBaseProfession() {
        assertTrue(BaseProfession.isBaseProfession("player"));
        assertTrue(BaseProfession.isBaseProfession("PLAYER"));
        assertFalse(BaseProfession.isBaseProfession("miner"));
        assertFalse(BaseProfession.isBaseProfession(null));
    }

    @Test
    @DisplayName("The base profession levels at a fixed cost with no cap")
    void levelling() {
        assertEquals("player", BaseProfession.CODE);
        assertEquals(86_400, BaseProfession.EXP_PER_LEVEL);
        assertEquals(Integer.MAX_VALUE, BaseProfession.MAX_LEVEL);
        assertEquals(100, BaseProfession.STAT_POINTS_PER_LEVEL);
    }
}
