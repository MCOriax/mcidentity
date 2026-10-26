package io.github.mcoriax.mcidentity.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The {@code agents} block of {@code config.yml}: the OpenRouter API key and
 * the model for each of the nine modalities this plugin's agent work runs on.
 *
 * <pre>
 * agents:
 *   token: global
 *   text: global
 *   image: global
 *   video: global
 *   speech: global
 *   decisions: global
 *   embeddings: global
 *   rerank: global
 *   transcription: global
 *   audio: global
 * </pre>
 *
 * <p>{@value #GLOBAL} — the shipped value of every key — means "whatever
 * MCAgents is configured with": the value is read at runtime from the same key
 * under {@code agents} in {@code plugins/MCAgents/config.yml}, through
 * {@link Global}. Any other value is used as written, so one plugin can run on
 * its own model or key while the rest of the server shares MCAgents'.</p>
 *
 * <p>A {@code config.yml} written before the modalities existed keeps working:
 * its {@code agents.model} is read as the {@code text} model.</p>
 *
 * @param token  The configured key; {@value #GLOBAL} when unset.
 * @param models The configured model per modality, lower-cased; {@value #GLOBAL} when unset.
 */
public record AgentsSettings(String token, Map<Modality, String> models) {

    /**
     * The value that defers a setting to MCAgents' own configuration.
     */
    public static final String GLOBAL = "global";

    /**
     * The key an older {@code config.yml} named the text model with.
     */
    public static final String LEGACY_MODEL_PATH = "agents.model";

    /**
     * The nine modalities a model can be chosen for, each read from
     * {@code agents.{key}}.
     */
    public enum Modality {
        /** Chat and text generation. */
        TEXT,
        /** Image generation and understanding. */
        IMAGE,
        /** Video generation and understanding. */
        VIDEO,
        /** Text to speech. */
        SPEECH,
        /** Short yes/no and classification decisions. */
        DECISIONS,
        /** Vector embeddings. */
        EMBEDDINGS,
        /** Reranking search results. */
        RERANK,
        /** Speech to text. */
        TRANSCRIPTION,
        /** Audio generation and understanding. */
        AUDIO;

        /**
         * Gets the key under {@code agents} this modality is read from.
         *
         * @return The lower-case key, such as {@code text}.
         */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Where {@value #GLOBAL} values are resolved: MCAgents' own {@code agents}
     * block.
     */
    public interface Global {

        /**
         * Gets MCAgents' model for a modality.
         *
         * @param modality The modality.
         * @return The model, blank when MCAgents is not available or names none.
         */
        String model(Modality modality);

        /**
         * Gets MCAgents' {@code agents.token}.
         *
         * @return The key, blank when MCAgents is not available or has none.
         */
        String token();
    }

    /**
     * Normalizes the values: a blank value is {@value #GLOBAL}, every modality
     * has an entry, and models are lower-cased so one model has one name.
     */
    public AgentsSettings {
        token = (token == null || token.isBlank()) ? GLOBAL : token.trim();
        Map<Modality, String> normalized = new EnumMap<>(Modality.class);
        for (Modality modality : Modality.values()) {
            String model = (models == null) ? null : models.get(modality);
            normalized.put(modality, (model == null || model.isBlank()) ? GLOBAL : model.trim().toLowerCase(Locale.ROOT));
        }
        models = Collections.unmodifiableMap(normalized);
    }

    /**
     * Reads the {@code agents} block, defaulting every value to {@value #GLOBAL}.
     *
     * @param config The root of {@code config.yml}.
     * @return The settings.
     */
    public static AgentsSettings read(ConfigurationSection config) {
        Map<Modality, String> models = new EnumMap<>(Modality.class);
        for (Modality modality : Modality.values()) {
            models.put(modality, config.getString("agents." + modality.key(), GLOBAL));
        }
        // isSet ignores the defaults Bukkit layers under the file, so only a
        // text key the operator wrote hides an older file's agents.model.
        if (!config.isSet("agents." + Modality.TEXT.key()) && config.isSet(LEGACY_MODEL_PATH)) {
            models.put(Modality.TEXT, config.getString(LEGACY_MODEL_PATH));
        }
        return new AgentsSettings(config.getString("agents.token", GLOBAL), models);
    }

    /**
     * Gets the configured model of one modality.
     *
     * @param modality The modality.
     * @return The model as written, or {@value #GLOBAL}.
     */
    public String model(Modality modality) {
        return this.models.get(modality);
    }

    /**
     * Reports whether a modality's model defers to MCAgents.
     *
     * @param modality The modality.
     * @return True when its model is {@value #GLOBAL}.
     */
    public boolean globalModel(Modality modality) {
        return GLOBAL.equalsIgnoreCase(model(modality));
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
     * Resolves the model one modality runs on.
     *
     * @param modality The modality.
     * @param global   MCAgents' own settings.
     * @return The configured model, or MCAgents' when it is {@value #GLOBAL};
     *         blank when it is global and MCAgents has none.
     */
    public String resolveModel(Modality modality, Global global) {
        if (!globalModel(modality)) {
            return model(modality);
        }
        String value = global.model(modality);
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
