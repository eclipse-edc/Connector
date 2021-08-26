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
                return VaultResponse.OK;
            }

            @Override
            public VaultResponse deleteSecret(String key) {
                return VaultResponse.OK;
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
