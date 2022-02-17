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

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.CertificateResolver;
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.Nullable;

/**
 * A vault extension fallback that gets loaded if no other implementation of the {@link VaultExtension} was found.
 */
public class NullVaultExtension implements VaultExtension {

    @Override
    public String name() {
        return "Null Vault";
    }


    @Override
    public Vault getVault() {
        return new Vault() {
            @Override
            public @Nullable String resolveSecret(String key) {
                return null;
            }

            @Override
            public Result<Void> storeSecret(String key, String value) {
                return Result.success();
            }

            @Override
            public Result<Void> deleteSecret(String key) {
                return Result.success();
            }
        };
    }

    @Override
    public PrivateKeyResolver getPrivateKeyResolver() {
        return new PrivateKeyResolver() {
            @Override
            public <T> @Nullable T resolvePrivateKey(String id, Class<T> keyType) {
                return null;
            }
        };
    }

    @Override
    public CertificateResolver getCertificateResolver() {
        return (key) -> null;
    }
}
