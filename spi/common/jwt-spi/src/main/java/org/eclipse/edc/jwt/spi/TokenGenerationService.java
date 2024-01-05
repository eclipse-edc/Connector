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

import java.util.function.Supplier;

/**
 * Interface for generating token based on a set of claims and header.
 */
public interface TokenGenerationService {

    /**
     * Generate a signed token based on the request.
     *
     * @param signatureInfoSupplier A {@link Supplier} that provides the private and an ID key on-demand. The ID can be used by verifiers to obtain the corresponding public key material.
     *                              Be advised that holding the private key in memory poses a considerable security risk and should be avoided.
     * @param decorators            an optional list of {@link JwtDecorator} objects to determine the shape of the token
     */
    Result<TokenRepresentation> generate(Supplier<SignatureInfo> signatureInfoSupplier, JwtDecorator... decorators);
}
