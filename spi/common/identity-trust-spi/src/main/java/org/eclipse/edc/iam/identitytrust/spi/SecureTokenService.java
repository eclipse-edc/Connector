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

package org.eclipse.edc.iam.identitytrust.spi;

import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A {@link SecureTokenService} is used to create self-signed ID tokens, that can contain a set of claims, and optionally, a
 * {@code bearerAccessScope}.
 */
public interface SecureTokenService {

    /**
     * Generates the self-signed ID token.
     *
     * @param claims            a set of claims, that are to be included in the SI token. MUST include {@code iss}, {@code sub} and {@code aud}.
     * @param bearerAccessScope if non-null, must be a space-separated list of scopes as per <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/M1/verifiable.presentation.protocol.md#31-access-scopes">DCP specification</a>
     *                          if bearerAccessScope != null -> creates a {@code token} claim, which is another JWT containing the scope as claims.
     *                          if bearerAccessScope == null -> creates a normal JWT using all the claims in the map
     */
    Result<TokenRepresentation> createToken(Map<String, Object> claims, @Nullable String bearerAccessScope);

}
