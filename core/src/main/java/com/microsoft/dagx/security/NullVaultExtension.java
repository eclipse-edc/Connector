package com.microsoft.dagx.security;

import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.VaultExtension;

/**
 * A vault extension stub.
 */
public class NullVaultExtension implements VaultExtension {
    @Override
    public Vault getVault() {
        return (key) -> null;
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
