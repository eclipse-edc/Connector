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

package org.eclipse.edc.iam.identitytrust.core.service;

import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Implementation of a {@link SecureTokenService}, that is capable of creating a self-signed ID token ("SI token") completely in-process.
 * To that end, it makes use of the <a href="https://connect2id.com/products/nimbus-jose-jwt">Nimbus JOSE/JWT library</a>.<br/>
 * As a recommendation, the private key it uses should not be used for anything else.
 */
public class EmbeddedSecureTokenService implements SecureTokenService {

    public EmbeddedSecureTokenService() {
    }

    @Override
    public Result<TokenRepresentation> createToken(Map<String, String> claims, @Nullable String bearerAccessScope) {
        // todo: implement embedded JWT generation
        return null;
    }
}
