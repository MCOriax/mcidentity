package io.github.mcoriax.mcidentity.tools;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Exposes a {@link PluginTool} to MCAgents without compiling against it.
 *
 * <p>The adapter loads {@value #AI_TOOL_CLASS} from the MCAgents plugin's class
 * loader and builds a {@link Proxy} implementing it that forwards every call to
 * the wrapped tool. The MCAgents {@code AIAccount} passed to {@code execute} is
 * read reflectively — {@code accountType()} and {@code accountUuid()} — into a
 * {@link ToolCaller}; an account that cannot be read becomes
 * {@link ToolCaller#UNKNOWN}, which every tool refuses.</p>
 */
public final class MCAgentsToolAdapter {

    /**
     * The binary name of the MCAgents tool interface.
     */
    public static final String AI_TOOL_CLASS = "io.github.mcpaimon.mcagents.tools.AITool";

    /**
     * The answer given when a tool throws instead of answering.
     */
    static final String TOOL_FAILED = "Error: The tool failed to run.";

    /**
     * Prevents instantiation: this class holds static factory methods only.
     */
    private MCAgentsToolAdapter() {
    }

    /**
     * Loads the MCAgents tool interface from the given class loader.
     * @param loader The MCAgents plugin's class loader.
     * @return The {@code AITool} interface.
     * @throws ClassNotFoundException When the loader does not provide it.
     */
    public static Class<?> loadToolInterface(ClassLoader loader) throws ClassNotFoundException {
        Class<?> type = Class.forName(AI_TOOL_CLASS, false, loader);
        if (!type.isInterface()) {
            throw new ClassNotFoundException(AI_TOOL_CLASS + " is not an interface");
        }
        return type;
    }

    /**
     * Builds a proxy of the MCAgents tool interface that forwards to a tool.
     * @param loader The MCAgents plugin's class loader.
     * @param tool The tool to expose.
     * @return An object implementing {@value #AI_TOOL_CLASS}.
     * @throws ClassNotFoundException When the loader does not provide the interface.
     */
    public static Object create(ClassLoader loader, PluginTool tool) throws ClassNotFoundException {
        return create(loadToolInterface(loader), tool);
    }

    /**
     * Builds a proxy of an already loaded tool interface that forwards to a tool.
     * @param toolInterface The MCAgents {@code AITool} interface.
     * @param tool The tool to expose.
     * @return An object implementing {@code toolInterface}.
     */
    public static Object create(Class<?> toolInterface, PluginTool tool) {
        Objects.requireNonNull(toolInterface, "toolInterface");
        Objects.requireNonNull(tool, "tool");
        return Proxy.newProxyInstance(toolInterface.getClassLoader(), new Class<?>[] {toolInterface},
            new Handler(tool));
    }

    /**
     * Reads the caller off an MCAgents {@code AIAccount}.
     * @param account The account object, of any type exposing {@code accountType()} and {@code accountUuid()}.
     * @return The caller, or {@link ToolCaller#UNKNOWN} when the account cannot be read.
     */
    public static ToolCaller toCaller(Object account) {
        if (account == null) {
            return ToolCaller.UNKNOWN;
        }
        try {
            Object type = account.getClass().getMethod("accountType").invoke(account);
            Object uuid = account.getClass().getMethod("accountUuid").invoke(account);
            return new ToolCaller(type == null ? null : type.toString(), uuid == null ? null : uuid.toString());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ToolCaller.UNKNOWN;
        }
    }

    /**
     * Forwards proxy calls to the wrapped tool.
     */
    private static final class Handler implements InvocationHandler {

        /**
         * The wrapped tool.
         */
        private final PluginTool tool;

        /**
         * Creates the handler.
         * @param tool The wrapped tool.
         */
        private Handler(PluginTool tool) {
            this.tool = tool;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            int arity = args == null ? 0 : args.length;
            switch (method.getName()) {
                case "getName":
                    if (arity == 0) return tool.getName();
                    break;
                case "getDescription":
                    if (arity == 0) return tool.getDescription();
                    break;
                case "getParametersJsonSchema":
                    if (arity == 0) return tool.getParametersJsonSchema();
                    break;
                case "getCategories":
                    if (arity == 0) return List.copyOf(tool.getCategories());
                    break;
                case "execute":
                    if (arity == 2) return execute(args[0], args[1]);
                    break;
                case "equals":
                    if (arity == 1) return proxy == args[0];
                    break;
                case "hashCode":
                    if (arity == 0) return System.identityHashCode(proxy);
                    break;
                case "toString":
                    if (arity == 0) return "MCIdentityTool[" + tool.getName() + "]";
                    break;
                default:
                    break;
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            throw new UnsupportedOperationException("MCIdentity tool does not implement " + method.getName());
        }

        /**
         * Runs the tool, turning a throw or a failed future into an error answer.
         * @param arguments The arguments map MCAgents passed.
         * @param account The MCAgents account object.
         * @return A future that always completes with an answer.
         */
        @SuppressWarnings("unchecked")
        private CompletableFuture<String> execute(Object arguments, Object account) {
            Map<String, Object> safeArguments = arguments instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
            try {
                CompletableFuture<String> result = tool.execute(safeArguments, toCaller(account));
                if (result == null) {
                    return CompletableFuture.completedFuture(TOOL_FAILED);
                }
                return result.exceptionally(ex -> TOOL_FAILED);
            } catch (RuntimeException e) {
                return CompletableFuture.completedFuture(TOOL_FAILED);
            }
        }
    }
}
