/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.vault;

import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.security.VaultResponse;
import org.jetbrains.annotations.Nullable;

public class SingletonVault implements Vault {

    public SingletonVault(String secret) {
        this.secrets = secret;
    }

    private final String secrets;

    @Override
    public @Nullable String resolveSecret(final String key) {
        return secrets;
    }

    @Override
    public VaultResponse storeSecret(
            final String key, final String value
    ) {
        // NOTE: Intentionally do nothing
        return VaultResponse.OK;
    }

    @Override
    public VaultResponse deleteSecret(final String key) {
        // NOTE: Intentionally do nothing
        return VaultResponse.OK;
    }
}
