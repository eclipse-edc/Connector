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

package org.eclipse.edc.identitytrust;

import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This interface is intended to give access to a SecureTokenService. The job of an STS is to create self-issued ID tokens.
 */
public interface SecureTokenService {

    /**
     * if bearerAccessScope != null -> creates an access_token claim, which is another JWT containing the scope as claims.
     * if bearerAccessScope == null -> creates a normal JWT using all the claims in the map
     */
    Result<TokenRepresentation> createToken(Map<String, String> claims, @Nullable String bearerAccessScope);

}
