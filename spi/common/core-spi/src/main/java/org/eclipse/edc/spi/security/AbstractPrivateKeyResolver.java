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

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.security.PrivateKey;

/**
 * Base class for private key resolvers, that handles the parsing of the key, but still leaves the actual resolution (e.g.
 * from a {@link Vault}) up to the inheritor.
 */
public abstract class AbstractPrivateKeyResolver implements PrivateKeyResolver {
    private final KeyParserRegistry registry;

    public AbstractPrivateKeyResolver(KeyParserRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Result<PrivateKey> resolvePrivateKey(String id) {
        var encodedKey = resolveInternal(id);
        if (encodedKey != null) {
            return registry.parse(encodedKey).compose(pk -> {
                if (pk instanceof PrivateKey privateKey) {
                    return Result.success(privateKey);
                } else return Result.failure("The specified resource did not contain private key material.");
            });
        }
        return Result.failure("No private key found for key-ID '%s'".formatted(id));
    }

    @Nullable
    protected abstract String resolveInternal(String keyId);
}
