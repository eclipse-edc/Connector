/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.security;

import org.jetbrains.annotations.Nullable;

/**
 * Implementation that returns private keys stored in a vault.
 */
public class VaultPrivateKeyResolver extends ConfigurablePrivateKeyResolver {

    private final Vault vault;

    public VaultPrivateKeyResolver(Vault vault, KeyParser<?>... parsers) {
        super(parsers);
        this.vault = vault;
    }

    public VaultPrivateKeyResolver(Vault vault) {
        super();
        this.vault = vault;
    }

    @Override
    protected @Nullable String getEncodedKey(String id) {
        return vault.resolveSecret(id);
    }
}
