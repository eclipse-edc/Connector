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

package org.eclipse.edc.keys;

import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.RetryableFailure;

import java.security.PublicKey;

/**
 * Base class for public key resolvers, that handles the parsing of the key, but still leaves the actual resolution (e.g.
 * from a DID document, or a URL) up to the inheritor.
 */
public abstract class AbstractPublicKeyResolver implements PublicKeyResolver {
    private final KeyParserRegistry registry;

    public AbstractPublicKeyResolver(KeyParserRegistry registry) {
        this.registry = registry;
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
                .recover(f -> {
                    var message = "No public key could be resolved for key-ID '%s': %s".formatted(id, f.getFailureDetail());
                    // keep telling apart a key that could not be resolved now (e.g. DID document not reachable)
                    return f instanceof RetryableFailure ? Result.retryableFailure(message) : Result.failure(message);
                });

    }

    protected abstract Result<String> resolveInternal(String id);

}
