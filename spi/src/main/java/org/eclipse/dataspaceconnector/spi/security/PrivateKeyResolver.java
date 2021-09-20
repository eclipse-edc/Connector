package org.eclipse.dataspaceconnector.spi.security;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface PrivateKeyResolver {
    String FEATURE = "edc:identity:private-key-resolver";

    /**
     * Returns the private key associated with the id or null if not found.
     */
    @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType);
}
