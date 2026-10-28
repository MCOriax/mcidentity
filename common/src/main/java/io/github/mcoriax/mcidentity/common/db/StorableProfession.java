package io.github.mcoriax.mcidentity.common.db;

import java.util.concurrent.CompletableFuture;

/**
 * The guard every backend applies before it stores a new identity.
 *
 * <p><strong>Internal.</strong> Package-private. {@code random} is resolved by
 * the provider before an identity reaches storage, so a backend that still
 * sees it, or sees no code at all, refuses the write.</p>
 */
final class StorableProfession {

    private StorableProfession() {
    }

    /**
     * Reports whether a profession code may be stored on an identity.
     * @param professionCode The code to check.
     * @return true when the code is present and is not {@code random}.
     */
    static boolean isStorable(String professionCode) {
        return professionCode != null && !professionCode.trim().isEmpty()
            && !professionCode.equalsIgnoreCase("random");
    }

    /**
     * Builds the failed future a backend returns for an unstorable code.
     * @param professionCode The rejected code.
     * @return A future failed with {@link IllegalArgumentException}.
     */
    static CompletableFuture<Void> rejected(String professionCode) {
        return CompletableFuture.failedFuture(
            new IllegalArgumentException("Invalid profession code: " + professionCode));
    }
}
