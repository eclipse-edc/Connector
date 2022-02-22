package org.eclipse.dataspaceconnector.spi.iam;

import org.jetbrains.annotations.Nullable;

import java.security.PublicKey;

/**
 * Resolves an RSA public key.
 */
@FunctionalInterface
public interface PublicKeyResolver {

    /**
     * Resolves the key or return null if not found.
     */
    @Nullable
    PublicKey resolveKey(String id);
}
