package io.github.mcoriax.mcidentity.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the proxy forwards the MCAgents tool contract to a {@link PluginTool}.
 */
class MCAgentsToolAdapterTest {

    /**
     * Stands in for {@code io.github.mcpaimon.mcagents.tools.AITool}.
     */
    public interface FakeAITool {
        String getName();
        String getDescription();
        String getParametersJsonSchema();
        List<String> getCategories();
        CompletableFuture<String> execute(Map<String, Object> arguments, FakeAccount account);
    }

    /**
     * Stands in for the MCAgents {@code AIAccount} record.
     */
    public record FakeAccount(String accountType, String accountUuid) {
    }

    /**
     * A tool that echoes the caller it was given.
     */
    private static class EchoTool implements PluginTool {
        @Override public String getName() { return "echo_tool"; }
        @Override public String getDescription() { return "Echoes the caller."; }
        @Override public String getParametersJsonSchema() { return "{\"type\":\"object\"}"; }
        @Override public List<String> getCategories() { return List.of("identity"); }
        @Override public CompletableFuture<String> execute(Map<String, Object> arguments, ToolCaller caller) {
            return CompletableFuture.completedFuture(caller.accountType() + ":" + caller.accountUuid() + ":" + arguments.size());
        }
    }

    @Test
    @DisplayName("The proxy forwards every contract method")
    void forwardsContract() {
        FakeAITool proxy = (FakeAITool) MCAgentsToolAdapter.create(FakeAITool.class, new EchoTool());

        assertEquals("echo_tool", proxy.getName());
        assertEquals("Echoes the caller.", proxy.getDescription());
        assertEquals("{\"type\":\"object\"}", proxy.getParametersJsonSchema());
        assertEquals(List.of("identity"), proxy.getCategories());
        assertEquals("player:abc:1", proxy.execute(Map.of("k", "v"), new FakeAccount("player", "abc")).join());
        assertEquals("null:null:0", proxy.execute(null, null).join());
    }

    @Test
    @DisplayName("Object methods behave sanely")
    void objectMethods() {
        Object first = MCAgentsToolAdapter.create(FakeAITool.class, new EchoTool());
        Object second = MCAgentsToolAdapter.create(FakeAITool.class, new EchoTool());

        assertEquals(first, first);
        assertNotEquals(first, second);
        assertEquals(System.identityHashCode(first), first.hashCode());
        assertTrue(first.toString().contains("echo_tool"));
    }

    @Test
    @DisplayName("A throwing tool answers with an error instead of throwing")
    void throwingToolDegrades() {
        PluginTool failing = new EchoTool() {
            @Override public CompletableFuture<String> execute(Map<String, Object> arguments, ToolCaller caller) {
                throw new IllegalStateException("boom");
            }
        };
        FakeAITool proxy = (FakeAITool) MCAgentsToolAdapter.create(FakeAITool.class, failing);

        assertEquals(MCAgentsToolAdapter.TOOL_FAILED, proxy.execute(Map.of(), new FakeAccount("player", "x")).join());
    }

    @Test
    @DisplayName("An unreadable account becomes the unknown caller")
    void unreadableAccount() {
        assertEquals(ToolCaller.UNKNOWN, MCAgentsToolAdapter.toCaller(new Object()));
        assertEquals(new ToolCaller("player", "u"), MCAgentsToolAdapter.toCaller(new FakeAccount("player", "u")));
    }

    @Test
    @DisplayName("A loader without MCAgents fails to load the interface")
    void missingInterface() {
        assertThrows(ClassNotFoundException.class,
            () -> MCAgentsToolAdapter.loadToolInterface(ClassLoader.getPlatformClassLoader()));
    }
}
