package io.github.mcoriax.mcidentity.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the identity tools refuse every caller but an online player and only
 * read that player's own identity.
 */
class IdentityToolsTest {

    /**
     * The online player the fake port knows.
     */
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * A port with one online player, recording every UUID it is asked about.
     */
    private static class FakePort implements IdentityToolPort {
        private final List<UUID> lookups = new ArrayList<>();
        private final Optional<IdentitySnapshot> identity;

        private FakePort(Optional<IdentitySnapshot> identity) {
            this.identity = identity;
        }

        @Override
        public CompletableFuture<Boolean> isOnlinePlayer(UUID playerUuid) {
            lookups.add(playerUuid);
            return CompletableFuture.completedFuture(PLAYER.equals(playerUuid));
        }

        @Override
        public CompletableFuture<Optional<IdentitySnapshot>> activeIdentity(UUID playerUuid) {
            lookups.add(playerUuid);
            return CompletableFuture.completedFuture(identity);
        }
    }

    private static IdentitySnapshot sample() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("Health", 20);
        stats.put("Strength", 7);
        return new IdentitySnapshot("id-1", "miner", "Miner", null, false,
            5, 100, 250, 1000, 3, stats, 18.5, 10, 20);
    }

    private static List<PluginTool> tools(IdentityToolPort port) {
        return List.of(new MyIdentityTool(port), new MyIdentityStatsTool(port));
    }

    @Test
    @DisplayName("Non-player and offline callers are refused")
    void refusesNonPlayers() {
        FakePort port = new FakePort(Optional.of(sample()));
        List<ToolCaller> callers = List.of(
            ToolCaller.UNKNOWN,
            new ToolCaller("server", PLAYER.toString()),
            new ToolCaller("player", "not-a-uuid"),
            new ToolCaller("player", UUID.randomUUID().toString()));
        for (PluginTool tool : tools(port)) {
            for (ToolCaller caller : callers) {
                assertEquals(PlayerToolSupport.NOT_A_PLAYER, tool.execute(Map.of(), caller).join(),
                    tool.getName() + " for " + caller);
            }
        }
    }

    @Test
    @DisplayName("Tools read only the caller's own identity, ignoring arguments")
    void readsOwnIdentityOnly() {
        FakePort port = new FakePort(Optional.of(sample()));
        Map<String, Object> sneaky = Map.of("player", UUID.randomUUID().toString());

        String identity = new MyIdentityTool(port).execute(sneaky, new ToolCaller("player", PLAYER.toString())).join();
        String stats = new MyIdentityStatsTool(port).execute(sneaky, new ToolCaller("PLAYER", PLAYER.toString())).join();

        assertTrue(identity.contains("Miner (miner)"), identity);
        assertTrue(identity.contains("Level: 5 (cap: 100)"), identity);
        assertTrue(identity.contains("EXP: 250 / 1000"), identity);
        assertTrue(stats.contains("Strength: 7"), stats);
        assertTrue(stats.contains("Current health: 18.5"), stats);
        assertTrue(port.lookups.stream().allMatch(PLAYER::equals), port.lookups.toString());
    }

    @Test
    @DisplayName("No equipped identity and a failed read both answer politely")
    void degradesSafely() {
        ToolCaller caller = new ToolCaller("player", PLAYER.toString());
        assertEquals(PlayerToolSupport.NO_IDENTITY,
            new MyIdentityTool(new FakePort(Optional.empty())).execute(Map.of(), caller).join());

        IdentityToolPort broken = new FakePort(Optional.empty()) {
            @Override
            public CompletableFuture<Optional<IdentitySnapshot>> activeIdentity(UUID playerUuid) {
                return CompletableFuture.failedFuture(new IllegalStateException("db down"));
            }
        };
        assertEquals(PlayerToolSupport.READ_FAILED, new MyIdentityStatsTool(broken).execute(Map.of(), caller).join());
    }

    @Test
    @DisplayName("Names are snake_case and schemas are valid JSON objects")
    void contractShape() {
        for (PluginTool tool : tools(new FakePort(Optional.empty()))) {
            assertTrue(tool.getName().matches("[a-z][a-z0-9_]*"), tool.getName());
            assertTrue(!tool.getDescription().isBlank());
            assertTrue(!tool.getCategories().isEmpty());
            String schema = tool.getParametersJsonSchema();
            assertEquals(schema.length(), new JsonCheck(schema).value(), schema);
            assertTrue(schema.contains("\"type\":\"object\""), schema);
        }
    }

    /**
     * A minimal strict JSON reader that returns where a value ends, or throws.
     */
    private static final class JsonCheck {
        private final String s;

        private JsonCheck(String s) {
            this.s = s;
        }

        int value() {
            int end = value(skip(0));
            return skip(end);
        }

        private int skip(int i) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            return i;
        }

        private int value(int i) {
            char c = s.charAt(i);
            if (c == '{') return container(i, '}', true);
            if (c == '[') return container(i, ']', false);
            if (c == '"') return string(i);
            for (String literal : new String[] {"true", "false", "null"}) {
                if (s.startsWith(literal, i)) return i + literal.length();
            }
            int j = i;
            while (j < s.length() && "+-0123456789.eE".indexOf(s.charAt(j)) >= 0) j++;
            if (j == i) throw new IllegalArgumentException("Unexpected '" + c + "' at " + i);
            return j;
        }

        private int container(int i, char close, boolean object) {
            i = skip(i + 1);
            if (s.charAt(i) == close) return i + 1;
            while (true) {
                if (object) {
                    i = skip(string(i));
                    if (s.charAt(i) != ':') throw new IllegalArgumentException("Expected ':' at " + i);
                    i = skip(i + 1);
                }
                i = skip(value(i));
                char c = s.charAt(i);
                if (c == close) return i + 1;
                if (c != ',') throw new IllegalArgumentException("Expected ',' at " + i);
                i = skip(i + 1);
            }
        }

        private int string(int i) {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("Expected string at " + i);
            for (int j = i + 1; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '\\') j++;
                else if (c == '"') return j + 1;
            }
            throw new IllegalArgumentException("Unterminated string");
        }
    }
}
