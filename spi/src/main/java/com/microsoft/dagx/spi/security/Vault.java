package com.microsoft.dagx.spi.security;

import org.jetbrains.annotations.Nullable;

/**
 * Provides secrets such as certificates and keys to the runtime.
 */
public interface Vault {

    /**
     * Resolve the secret for the given key.
     *
     * @param key the key
     * @return the key as a string or null if not found. Binary data will be Base 64 encoded.
     */
    @Nullable
    String resolveSecret(String key);

    /**
     * Saves a secret.
     *
     * @param key the secret key
     * @param value the serialized secret value
     */
    VaultResponse storeSecret(String key, String value);

    /**
     * Deletes a secret. Depending on the vault implementation, this might mean a soft delete, or no be even permissible.
     * @param key the secret's key
     */
    VaultResponse deleteSecret(String key);
}
