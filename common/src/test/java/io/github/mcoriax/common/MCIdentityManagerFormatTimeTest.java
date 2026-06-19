package io.github.mcoriax.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the human-readable duration formatter on {@link MCIdentityManager}.
 */
class MCIdentityManagerFormatTimeTest {

    @Test
    @DisplayName("Test time formatting")
    void testFormatTime() {
        assertEquals("0s", MCIdentityManager.formatTime(0));
        assertEquals("1s", MCIdentityManager.formatTime(1000));
        assertEquals("1m", MCIdentityManager.formatTime(60000));
        assertEquals("1m 1s", MCIdentityManager.formatTime(61000));
        assertEquals("59s", MCIdentityManager.formatTime(59000));
    }
}
