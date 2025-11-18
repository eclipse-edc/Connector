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

package org.eclipse.edc.keys;

import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;

/**
 * Base class for private key resolvers, that handles the parsing of the key, but still leaves the actual resolution (e.g.
 * from a {@link Vault}) up to the inheritor.
 */
public abstract class AbstractPrivateKeyResolver implements PrivateKeyResolver {
    private final KeyParserRegistry registry;
    private final Config config;
    private final Monitor monitor;

    public AbstractPrivateKeyResolver(KeyParserRegistry registry, Config config, Monitor monitor) {
        this.registry = registry;
        this.config = config;
        this.monitor = monitor;
    }

    @Override
    public Result<PrivateKey> resolvePrivateKey(String id) {
        return resolvePrivateKey(null, id);
    }

    @Override
    public Result<PrivateKey> resolvePrivateKey(String participantContextId, String id) {
        var encodedKeyResult = resolveInternal(participantContextId, id);

        return encodedKeyResult
                .recover(failure -> {
                    monitor.debug("Public key not found, fallback to config. Error: %s".formatted(failure.getFailureDetail()));
                    return resolveFromConfig(id);
                })
                .compose(encodedKey -> registry.parse(encodedKey).compose(pk -> {
                    if (pk instanceof PrivateKey privateKey) {
                        return Result.success(privateKey);
                    } else {
                        var msg = "The specified resource did not contain private key material.";
                        monitor.warning(msg);
                        return Result.failure(msg);
                    }
                }));
    }

    /**
     * Returns the resolved key material
     *
     * @param keyId the Key-ID
     * @return {@link Result#success()} if the key was found, {@link Result#failure(String)} if not found or other error.
     */
    @NotNull
    protected abstract Result<String> resolveInternal(String participantContextId, String keyId);

    private Result<String> resolveFromConfig(String keyId) {
        var value = config.getString(keyId, null);
        return value == null ?
                Result.failure("Private key with ID '%s' not found in Config".formatted(keyId)) :
                Result.success(value);
    }
}
