/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.system;

import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;

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

    default void intializeVault(ServiceExtensionContext context) {
    }

    ;
}
