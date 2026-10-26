package io.github.mcoriax.mcidentity.bukkit.config;

import io.github.mcoriax.mcidentity.bukkit.config.AgentsSettings.Modality;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves how the {@code agents} block is read and how {@code global} values are
 * resolved against MCAgents' own settings.
 */
class AgentsSettingsTest {

    /**
     * MCAgents' settings as a test sees them: each modality answers with its
     * own model so a mix-up shows.
     */
    private static final AgentsSettings.Global MCAGENTS = new AgentsSettings.Global() {
        @Override
        public String model(Modality modality) {
            return "OpenRouter/" + modality.key();
        }

        @Override
        public String token() {
            return "sk-or-global";
        }
    };

    /**
     * Parses a configuration file body.
     *
     * @param yaml The file body.
     * @return The settings read from it.
     * @throws InvalidConfigurationException When the body is not valid YAML.
     */
    private static AgentsSettings read(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return AgentsSettings.read(config);
    }

    @Test
    void globalValuesResolveFromMCAgents() throws InvalidConfigurationException {
        AgentsSettings settings = read("agents:\n  token: global\n  text: global\n  embeddings: global\n");

        assertTrue(settings.globalToken());
        for (Modality modality : Modality.values()) {
            assertTrue(settings.globalModel(modality), modality.key());
            assertEquals("openrouter/" + modality.key(), settings.resolveModel(modality, MCAGENTS));
        }
        assertEquals("sk-or-global", settings.resolveToken(MCAGENTS));
    }

    @Test
    void ownValuesAreUsedAsWritten() throws InvalidConfigurationException {
        AgentsSettings settings = read("agents:\n  token: sk-or-own\n  text: OpenAI/GPT-5\n"
                + "  embeddings: openai/text-embedding-3-small\n");

        assertFalse(settings.globalToken());
        assertEquals("openai/gpt-5", settings.resolveModel(Modality.TEXT, MCAGENTS));
        assertEquals("openai/text-embedding-3-small", settings.resolveModel(Modality.EMBEDDINGS, MCAGENTS));
        assertEquals("openrouter/image", settings.resolveModel(Modality.IMAGE, MCAGENTS));
        assertEquals("sk-or-own", settings.resolveToken(MCAGENTS));
    }

    @Test
    void legacyModelIsTheTextModel() throws InvalidConfigurationException {
        AgentsSettings legacy = read("agents:\n  model: OpenAI/GPT-5\n  token: global\n");
        assertEquals("openai/gpt-5", legacy.model(Modality.TEXT));
        assertEquals(AgentsSettings.GLOBAL, legacy.model(Modality.AUDIO));

        AgentsSettings both = read("agents:\n  model: old/model\n  text: new/model\n");
        assertEquals("new/model", both.model(Modality.TEXT));
    }

    @Test
    void missingBlockDefaultsToGlobal() throws InvalidConfigurationException {
        AgentsSettings settings = read("enable: true\n");

        assertEquals(AgentsSettings.GLOBAL, settings.token());
        for (Modality modality : Modality.values()) {
            assertEquals(AgentsSettings.GLOBAL, settings.model(modality));
        }
    }

    @Test
    void globalWithoutMCAgentsResolvesBlank() throws InvalidConfigurationException {
        AgentsSettings settings = read("agents:\n  token: Global\n  text: GLOBAL\n");
        AgentsSettings.Global absent = new AgentsSettings.Global() {
            @Override
            public String model(Modality modality) {
                return "";
            }

            @Override
            public String token() {
                return null;
            }
        };

        assertEquals("", settings.resolveModel(Modality.TEXT, absent));
        assertEquals("", settings.resolveToken(absent));
    }
}
