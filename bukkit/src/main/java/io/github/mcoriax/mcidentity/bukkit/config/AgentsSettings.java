package io.github.mcoriax.mcidentity.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * The {@code agents} block of {@code config.yml}: which AI model and which
 * OpenRouter API key this plugin's agent work runs on.
 *
 * <pre>
 * agents:
 *   model: global
 *   token: global
 * </pre>
 *
 * <p>{@value #GLOBAL} — the shipped value of both — means "whatever MCAgents is
 * configured with": the value is read at runtime from {@code agents.model} or
 * {@code agents.token} in {@code plugins/MCAgents/config.yml}, through
 * {@link Global}. Any other value is used as written, so one plugin can run on
 * its own model or key while the rest of the server shares MCAgents'.</p>
 *
 * @param model The configured model, lower-cased; {@value #GLOBAL} when unset.
 * @param token The configured key; {@value #GLOBAL} when unset.
 */
public record AgentsSettings(String model, String token) {

    /**
     * The value that defers a setting to MCAgents' own configuration.
     */
    public static final String GLOBAL = "global";

    /**
     * Where {@value #GLOBAL} values are resolved: MCAgents' own
     * {@code agents.model} and {@code agents.token}.
     */
    public interface Global {

        /**
         * Gets MCAgents' {@code agents.model}.
         *
         * @return The model, blank when MCAgents is not available.
         */
        String model();

        /**
         * Gets MCAgents' {@code agents.token}.
         *
         * @return The key, blank when MCAgents is not available or has none.
         */
        String token();
    }

    /**
     * Normalizes the values: a blank value is {@value #GLOBAL}, and the model
     * is lower-cased so one model has one name.
     */
    public AgentsSettings {
        model = (model == null || model.isBlank()) ? GLOBAL : model.trim().toLowerCase(Locale.ROOT);
        token = (token == null || token.isBlank()) ? GLOBAL : token.trim();
    }

    /**
     * Reads the {@code agents} block, defaulting both values to {@value #GLOBAL}.
     *
     * @param config The root of {@code config.yml}.
     * @return The settings.
     */
    public static AgentsSettings read(ConfigurationSection config) {
        return new AgentsSettings(config.getString("agents.model", GLOBAL), config.getString("agents.token", GLOBAL));
    }

    /**
     * Reports whether the model defers to MCAgents.
     *
     * @return True when {@code agents.model} is {@value #GLOBAL}.
     */
    public boolean globalModel() {
        return GLOBAL.equalsIgnoreCase(this.model);
    }

    /**
     * Reports whether the key defers to MCAgents.
     *
     * @return True when {@code agents.token} is {@value #GLOBAL}.
     */
    public boolean globalToken() {
        return GLOBAL.equalsIgnoreCase(this.token);
    }

    /**
     * Resolves the model this plugin runs on.
     *
     * @param global MCAgents' own settings.
     * @return The configured model, or MCAgents' when it is {@value #GLOBAL};
     *         blank when it is global and MCAgents has none.
     */
    public String resolveModel(Global global) {
        if (!globalModel()) {
            return this.model;
        }
        String value = global.model();
        return (value == null) ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the OpenRouter API key this plugin runs on.
     *
     * @param global MCAgents' own settings.
     * @return The configured key, or MCAgents' when it is {@value #GLOBAL};
     *         blank when it is global and MCAgents has none.
     */
    public String resolveToken(Global global) {
        if (!globalToken()) {
            return this.token;
        }
        String value = global.token();
        return (value == null) ? "" : value.trim();
    }
}
