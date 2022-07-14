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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation that returns private keys stored in a vault.
 */
public class VaultPrivateKeyResolver extends AbstractPrivateKeyResolver {

    private final Vault vault;
    private final List<KeyParser<?>> parsers;

    public VaultPrivateKeyResolver(Vault vault, KeyParser<?>... parsers) {
        super(parsers);
        this.vault = vault;
        this.parsers = Arrays.asList(parsers);
    }

    public VaultPrivateKeyResolver(Vault vault) {
        // can't use this(vault) here because properties are final
        this.vault = vault;
        parsers = new ArrayList<>();
    }

    @Override
    public @Nullable <T> T resolvePrivateKey(String id, Class<T> keyType) {
        var encodedKey = vault.resolveSecret(id);

        if (encodedKey == null) {
            return null;
        }

        return keyType.cast(getParser(keyType).parse(encodedKey));
    }

}
