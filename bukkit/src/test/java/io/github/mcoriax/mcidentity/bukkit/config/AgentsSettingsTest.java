package io.github.mcoriax.mcidentity.bukkit.config;

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
     * MCAgents' settings as a test sees them.
     */
    private static final AgentsSettings.Global MCAGENTS = new AgentsSettings.Global() {
        @Override
        public String model() {
            return "OpenRouter/Free";
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
        AgentsSettings settings = read("agents:\n  model: global\n  token: global\n");

        assertTrue(settings.globalModel());
        assertTrue(settings.globalToken());
        assertEquals("openrouter/free", settings.resolveModel(MCAGENTS));
        assertEquals("sk-or-global", settings.resolveToken(MCAGENTS));
    }

    @Test
    void ownValuesAreUsedAsWritten() throws InvalidConfigurationException {
        AgentsSettings settings = read("agents:\n  model: OpenAI/GPT-5\n  token: sk-or-own\n");

        assertFalse(settings.globalModel());
        assertFalse(settings.globalToken());
        assertEquals("openai/gpt-5", settings.resolveModel(MCAGENTS));
        assertEquals("sk-or-own", settings.resolveToken(MCAGENTS));
    }

    @Test
    void missingBlockDefaultsToGlobal() throws InvalidConfigurationException {
        AgentsSettings settings = read("enable: true\n");

        assertEquals(AgentsSettings.GLOBAL, settings.model());
        assertEquals(AgentsSettings.GLOBAL, settings.token());
    }

    @Test
    void globalWithoutMCAgentsResolvesBlank() throws InvalidConfigurationException {
        AgentsSettings settings = read("agents:\n  model: GLOBAL\n  token: Global\n");
        AgentsSettings.Global absent = new AgentsSettings.Global() {
            @Override
            public String model() {
                return "";
            }

            @Override
            public String token() {
                return null;
            }
        };

        assertEquals("", settings.resolveModel(absent));
        assertEquals("", settings.resolveToken(absent));
    }
}
