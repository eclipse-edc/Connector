package com.microsoft.dagx.spi.security;

import org.jetbrains.annotations.Nullable;

import java.security.interfaces.RSAPrivateKey;

/**
 * Resolves RSA private keys.
 */
public interface PrivateKeyResolver {

    /**
     * Returns the private key associated with the id or null if not found.
     */
    @Nullable
    RSAPrivateKey resolvePrivateKey(String id);

}
