package com.microsoft.dagx.security;

import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
import com.microsoft.dagx.spi.system.VaultExtension;
import org.jetbrains.annotations.Nullable;

/**
 * A vault extension stub.
 */
public class NullVaultExtension implements VaultExtension {
    @Override
    public Vault getVault() {
        return new Vault() {
            @Override
            public @Nullable String resolveSecret(String key) {
                return null;
            }

            @Override
            public VaultResponse storeSecret(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public VaultResponse deleteSecret(String key) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return (key) -> null;
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return (key) -> null;
    }
}
