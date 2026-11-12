package io.github.mcoriax.mcidentity.tools;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves every tool names a permission node and that MCAgents can read it
 * through the adapter's proxy.
 */
class ToolPermissionTest {

    /** Stand-in for the part of MCAgents' AITool the node is read through. */
    public interface SampleAITool {
        String getName();

        String getPermission();
    }

    private static PluginTool sampleTool() {
        return (PluginTool) Proxy.newProxyInstance(PluginTool.class.getClassLoader(), new Class<?>[] {PluginTool.class},
                (InvocationHandler) (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    return "getName".equals(method.getName()) ? "sample_tool" : null;
                });
    }

    @Test
    void aToolWithoutItsOwnNodeGetsThePluginPrefix() {
        assertEquals(PluginTool.PERMISSION_PREFIX + "sample_tool", sampleTool().getPermission());
    }

    @Test
    void theAdapterAnswersGetPermission() throws Exception {
        Method create = Arrays.stream(MCAgentsToolAdapter.class.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Arrays.equals(m.getParameterTypes(), new Class<?>[] {Class.class, PluginTool.class}))
                .findFirst().orElseThrow();

        SampleAITool aiTool = (SampleAITool) create.invoke(null, SampleAITool.class, sampleTool());

        assertEquals(PluginTool.PERMISSION_PREFIX + "sample_tool", aiTool.getPermission());
    }
}
