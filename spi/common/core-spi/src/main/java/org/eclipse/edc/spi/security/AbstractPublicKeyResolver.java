/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.security;

import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;

import java.security.PublicKey;

/**
 * Base class for public key resolvers, that handles the parsing of the key, but still leaves the actual resolution (e.g.
 * from a DID document, or a URL) up to the inheritor.
 */
public abstract class AbstractPublicKeyResolver implements PublicKeyResolver {
    private final KeyParserRegistry registry;
    private final Config config;
    private final Monitor monitor;

    public AbstractPublicKeyResolver(KeyParserRegistry registry, Config config, Monitor monitor) {
        this.registry = registry;
        this.config = config;
        this.monitor = monitor;
    }

    @Override
    public Result<PublicKey> resolveKey(String id) {
        var encodedKeyResult = resolveInternal(id);
        return encodedKeyResult
                .compose(encodedKey ->
                        registry.parse(encodedKey).compose(pk -> {
                            if (pk instanceof PublicKey publicKey) {
                                return Result.success(publicKey);
                            } else return Result.failure("The specified resource did not contain public key material.");
                        }))
                .recover(f -> Result.failure("No public key could be resolved for key-ID '%s': %s".formatted(id, f.getFailureDetail())));

    }

    protected abstract Result<String> resolveInternal(String id);

    private String resolveFromConfig(String keyId) {
        return config.getString(keyId, null);
    }
}
