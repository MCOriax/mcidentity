package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.mockito.Mockito.mock;

/**
 * Shared fixtures for {@link MCIdentityProvider} tests.
 * <p>
 * The provider suite is split across focused subclasses (schema, identity,
 * session, query, mutation, state-proxy) to keep each file small; they all
 * share this mocked database and clean RAM state.
 * </p>
 */
abstract class ProviderTestSupport {

    protected IMCIdentityDB mockDatabase;
    protected MCIdentityProvider provider;
    protected UUID identityId;
    protected UUID playerId;

    @BeforeEach
    void setUpProvider() {
        mockDatabase = mock(IMCIdentityDB.class);
        provider = new MCIdentityProvider(mockDatabase);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        MCIdentityManager.clear();
    }
}
