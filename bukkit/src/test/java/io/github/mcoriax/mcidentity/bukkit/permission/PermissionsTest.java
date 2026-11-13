package io.github.mcoriax.mcidentity.bukkit.permission;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves every node {@link Permissions} checks is declared in each of the four
 * {@code plugin.yml} files, that the wildcard lists every child, and how the
 * profession wearing nodes are checked.
 */
class PermissionsTest {

    /**
     * The modules that ship a {@code plugin.yml}.
     */
    private static final List<String> PLATFORMS = List.of("engine", "spigotmc", "papermc", "foliamc");

    /**
     * The nodes that must default to operators only.
     */
    private static final Set<String> OPERATOR_NODES = Set.of(Permissions.ALL);

    /**
     * Reads the {@code permissions} section of one platform's {@code plugin.yml}.
     * @param platform The platform module name.
     * @return The section, keyed by node.
     * @throws IOException When the file cannot be read.
     * @throws InvalidConfigurationException When the file is not valid YAML.
     */
    private static ConfigurationSection permissionsOf(String platform)
            throws IOException, InvalidConfigurationException {
        Path file = Path.of("..", "platforms", platform, "src", "main", "resources", "plugin.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        // Node names contain dots, so the path separator must be something else.
        yaml.options().pathSeparator('/');
        yaml.loadFromString(Files.readString(file));
        ConfigurationSection permissions = yaml.getConfigurationSection("permissions");
        assertNotNull(permissions, platform + " plugin.yml declares no permissions");
        return permissions;
    }

    /**
     * Lists every node constant on {@link Permissions}.
     * @return The node values, the bare prefix excluded.
     * @throws IllegalAccessException Never; the fields are public.
     */
    private static List<String> declaredNodes() throws IllegalAccessException {
        List<String> nodes = new ArrayList<>();
        for (Field field : Permissions.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class
                    && !field.getName().equals("ROOT")) {
                nodes.add((String) field.get(null));
            }
        }
        return nodes;
    }

    /**
     * Builds a sender holding exactly the given nodes.
     * @param held The nodes the sender holds.
     * @return A permissible answering only for those nodes.
     */
    private static Permissible holding(String... held) {
        Set<String> nodes = Set.of(held);
        return (Permissible) Proxy.newProxyInstance(Permissible.class.getClassLoader(),
                new Class<?>[]{Permissible.class},
                (proxy, method, args) -> method.getName().equals("hasPermission")
                        && args[0] instanceof String node && nodes.contains(node));
    }

    @Test
    void everyNodeIsDeclaredWithDescriptionAndDefaultOnEveryPlatform() throws Exception {
        for (String platform : PLATFORMS) {
            ConfigurationSection permissions = permissionsOf(platform);
            for (String node : declaredNodes()) {
                ConfigurationSection entry = permissions.getConfigurationSection(node);
                assertNotNull(entry, platform + " does not declare " + node);
                assertFalse(entry.getString("description", "").isBlank(), platform + " " + node + " has no description");
                String expected = OPERATOR_NODES.contains(node) ? "op" : "true";
                assertEquals(expected, entry.getString("default"), platform + " " + node + " default");
            }
        }
    }

    @Test
    void wildcardListsEveryNode() throws Exception {
        for (String platform : PLATFORMS) {
            ConfigurationSection permissions = permissionsOf(platform);
            ConfigurationSection all = permissions.getConfigurationSection(Permissions.ALL + "/children");
            assertNotNull(all, platform + " wildcard has no children");
            for (String node : Permissions.SUB_COMMANDS.values()) {
                assertTrue(all.getBoolean(node), platform + " wildcard misses " + node);
            }
            assertTrue(all.getBoolean(Permissions.USE), platform + " wildcard misses " + Permissions.USE);
            assertTrue(all.getBoolean(Permissions.PROFESSION_ALL), platform + " wildcard misses " + Permissions.PROFESSION_ALL);
        }
    }

    @Test
    void grantsChecksTheNodeItself() {
        Permissible player = holding(Permissions.WEAR);

        assertTrue(Permissions.grants(player, Permissions.WEAR));
        assertFalse(Permissions.grants(player, Permissions.UNWEAR));
    }

    @Test
    void wearingNeedsTheWildcardOrTheProfessionNode() {
        Permissible everything = holding(Permissions.PROFESSION_ALL);
        Permissible minerOnly = holding("mcoriax.mcidentity.profession.miner");
        Permissible nothing = holding();

        assertTrue(Permissions.canWear(everything, "fighter"));
        assertTrue(Permissions.canWear(minerOnly, "miner"));
        assertFalse(Permissions.canWear(minerOnly, "fighter"));
        assertTrue(Permissions.canWear(nothing, "player"), "the base profession is always wearable");
        assertFalse(Permissions.canWear(nothing, "miner"));
    }

    @Test
    void everySubCommandHasItsOwnNode() {
        assertEquals(Permissions.SUB_COMMANDS.size(), Set.copyOf(Permissions.SUB_COMMANDS.values()).size());
        assertEquals(Permissions.WEAR, Permissions.SUB_COMMANDS.get("wear"));
        assertEquals(Permissions.UNWEAR, Permissions.SUB_COMMANDS.get("unwear"));
    }
}
