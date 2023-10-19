/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.jwt;

import org.eclipse.edc.jwt.spi.JwtDecorator;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.util.Objects;

/**
 * Token generator that wraps {@link TokenGenerationServiceImpl} and does not cache the private
 * key, but instead it resolves it at token generation time.
 */
public class LazyTokenGenerationService implements TokenGenerationService {

    private final PrivateKeyResolver privateKeyResolver;
    private final String keyAlias;

    public LazyTokenGenerationService(PrivateKeyResolver privateKeyResolver, String keyAlias) {
        this.privateKeyResolver = Objects.requireNonNull(privateKeyResolver);
        this.keyAlias = Objects.requireNonNull(keyAlias);
    }

    @Override
    public Result<TokenRepresentation> generate(@NotNull JwtDecorator... decorators) {
        var key = privateKeyResolver.resolvePrivateKey(keyAlias, PrivateKey.class);
        return new TokenGenerationServiceImpl(key).generate(decorators);
    }
}
