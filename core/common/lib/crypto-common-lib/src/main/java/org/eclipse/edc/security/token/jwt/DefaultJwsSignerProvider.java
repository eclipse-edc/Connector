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

package org.eclipse.edc.security.token.jwt;

import com.nimbusds.jose.JWSSigner;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.spi.result.Result;

/**
 * Provides a {@link JWSSigner} that is created based on a private key's algorithm.
 * Note that the private key will be held in memory for the duration of the instantiation of the {@link JWSSigner}.
 */
public class DefaultJwsSignerProvider implements JwsSignerProvider {

    private final PrivateKeyResolver privateKeyResolver;

    public DefaultJwsSignerProvider(PrivateKeyResolver privateKeyResolver) {
        this.privateKeyResolver = privateKeyResolver;
    }

    @Override
    public Result<JWSSigner> createJwsSigner(String privateKeyId) {
        return privateKeyResolver.resolvePrivateKey(privateKeyId)
                       .compose(pk -> Result.ofThrowable(() -> CryptoConverter.createSignerFor(pk)));
    }

    @Override
    public Result<JWSSigner> createJwsSigner(String participantContextId, String privateKeyId) {
        return privateKeyResolver.resolvePrivateKey(participantContextId, privateKeyId)
                       .compose(pk -> Result.ofThrowable(() -> CryptoConverter.createSignerFor(pk)));
    }
}
