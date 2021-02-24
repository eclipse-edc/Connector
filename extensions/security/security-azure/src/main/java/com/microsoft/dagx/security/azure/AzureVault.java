package com.microsoft.dagx.security.azure;

import com.microsoft.dagx.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

/**
 * Implements a vault backed by Azure Vault.
 */
public class AzureVault implements Vault {

    @Override
    public @Nullable String resolveSecret(String key) {
        // TODO: implement
        return null;
    }
}
