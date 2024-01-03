/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.jwt.spi;

import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

import java.security.PrivateKey;
import java.util.function.Supplier;

/**
 * Interface for generating token based on a set of claims and header.
 */
public interface TokenGenerationService {

    /**
     * Generate a signed token based on the request.
     *
     * @param privateKeySupplier A {@link Supplier} that provides the private key on-demand. Be advised to not hold the private key
     *                           in memory, as that poses a considerable security risk.
     * @param decorators         an optional list of {@link JwtDecorator} objects to determine the shape of the token
     */
    Result<TokenRepresentation> generate(Supplier<PrivateKey> privateKeySupplier, JwtDecorator... decorators);
}
