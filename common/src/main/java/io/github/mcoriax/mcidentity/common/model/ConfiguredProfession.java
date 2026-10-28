package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;

import java.util.Map;

/**
 * A stored profession whose configuration accessors read the values loaded
 * into {@link MCIdentityManager} from the local profession files.
 *
 * <p>The storage backends only hold a profession's code and display name; the
 * rest of a profession lives in its YAML file, so every backend returned this
 * same view as its own anonymous class.</p>
 *
 * @param systemCode The profession system code.
 * @param displayName The profession display name.
 */
public record ConfiguredProfession(String systemCode, String displayName) implements Profession {

    /** {@inheritDoc} */
    @Override
    public String getSystemCode() {
        return systemCode;
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getExpConfig() {
        return MCIdentityManager.getProfessionExpConfig(systemCode);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Integer> getBaseStats() {
        return MCIdentityManager.getProfessionBaseStats(systemCode);
    }

    /** {@inheritDoc} */
    @Override
    public String getStatsRandom() {
        return MCIdentityManager.getProfessionStatsRandom(systemCode);
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, String> getSkins() {
        return MCIdentityManager.getProfessionSkins(systemCode);
    }

    /** {@inheritDoc} */
    @Override
    public void setSkins(Map<Integer, String> skins) {
        MCIdentityManager.setProfessionSkins(systemCode, skins);
    }
}
