package com.microsoft.dagx.security.azure;

import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
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

    @Override
    public VaultResponse storeSecret(String key, String value) {
        return null;
    }
}
