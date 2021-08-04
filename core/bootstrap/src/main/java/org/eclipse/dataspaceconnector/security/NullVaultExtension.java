/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.security;

import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.eclipse.dataspaceconnector.spi.system.VaultExtension;
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
