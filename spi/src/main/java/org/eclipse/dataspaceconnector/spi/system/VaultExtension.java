/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;

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
