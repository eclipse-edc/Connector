package com.microsoft.dagx.spi.system;

import com.microsoft.dagx.spi.security.CertificateResolver;
import com.microsoft.dagx.spi.security.PrivateKeyResolver;
import com.microsoft.dagx.spi.security.Vault;

/**
 * Provides a vault to resolve secrets.
 */
public interface VaultExtension extends BootExtension {

    /**
     * Returns the vault.
     */
    Vault getVault();

    /**
     * Returns the private key resolver.
     */
    PrivateKeyResolver getPrivateKeyResolver();

    /**
     * Returns the certificate resolver.
     */
    CertificateResolver getCertificateResolver();

}
