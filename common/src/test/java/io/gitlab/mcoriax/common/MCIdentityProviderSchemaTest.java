package io.gitlab.mcoriax.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

/**
 * Schema-initialization delegation test for {@link MCIdentityProvider}.
 */
class MCIdentityProviderSchemaTest extends ProviderTestSupport {

    @Test
    @DisplayName("Test schema initialization")
    void testInitializeSchema() {
        when(mockDatabase.initializeSchema()).thenReturn(CompletableFuture.completedFuture(null));
        provider.initializeSchema().join();
        verify(mockDatabase).initializeSchema();
    }
}
