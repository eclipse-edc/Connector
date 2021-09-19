package org.eclipse.dataspaceconnector.spi.security;

import org.jetbrains.annotations.Nullable;

public interface PrivateKeyResolver<T> {
    String FEATURE = "edc:identity:private-key-resolver";

    /**
     * Returns the private key associated with the id or null if not found.
     */
    @Nullable
    T resolvePrivateKey(String id);
}
