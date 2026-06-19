package io.github.mcoriax.foliamc;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for the Folia {@link MCIdentityPlugin} entry point.
 * <p>
 * The plugin wires Bukkit/Folia services that need a live server, so rather than
 * booting one we assert the structural contract a server relies on: a concrete,
 * public {@link JavaPlugin} that declares the enable/disable lifecycle hooks.
 * </p>
 */
class MCIdentityPluginStructureTest {

    @Test
    @DisplayName("MCIdentityPlugin is a concrete JavaPlugin exposing lifecycle hooks")
    void testPluginContract() throws NoSuchMethodException {
        Class<MCIdentityPlugin> type = MCIdentityPlugin.class;

        assertTrue(JavaPlugin.class.isAssignableFrom(type), "plugin must extend JavaPlugin");
        assertTrue(Modifier.isPublic(type.getModifiers()), "plugin must be public");
        assertFalse(Modifier.isAbstract(type.getModifiers()), "plugin must be instantiable");
        assertNotNull(type.getDeclaredMethod("onEnable"));
        assertNotNull(type.getDeclaredMethod("onDisable"));
    }
}
