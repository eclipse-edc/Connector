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

package org.eclipse.edc.token.spi;

import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

/**
 * Interface for generating token based on a set of claims and header.
 */
public interface TokenGenerationService {

    /**
     * Generate a signed token based on the request.
     *
     * @param privateKeyId A String that identifies the private key, e.g. a secret alias in a {@link org.eclipse.edc.spi.security.Vault}
     * @param decorators   an optional list of {@code JwtDecorator} objects to determine the shape of the token, i.e. headers and claims
     * @deprecated Please {@link #generate(String, String, TokenDecorator...)} instead.
     */
    @Deprecated(since = "0.15.0")
    Result<TokenRepresentation> generate(String privateKeyId, TokenDecorator... decorators);

    /**
     * Generate a signed token using the private key as specified.
     *
     * @param participantContextId the context id of the participant generating the token.
     * @param privateKeyId         A String that identifies the private key, e.g. a secret alias in a {@link org.eclipse.edc.spi.security.Vault}
     * @param decorators           an optional list of {@code JwtDecorator} objects to determine the shape of the token, i.e., headers and claims
     */
    Result<TokenRepresentation> generate(String participantContextId, String privateKeyId, TokenDecorator... decorators);
}
