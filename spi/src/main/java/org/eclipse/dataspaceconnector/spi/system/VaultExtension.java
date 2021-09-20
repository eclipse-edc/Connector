/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
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

}
