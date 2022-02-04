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
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Feature;

/**
 * Obtains client security tokens from an identity provider.
 * Providers may implement different authorization protocols such as OAuth2.
 */
@Feature(IdentityService.FEATURE)
public interface IdentityService {

    String FEATURE = "edc:iam";

    /**
     * Obtains a client token encoded as a JWT.
     */
    Result<TokenRepresentation> obtainClientCredentials(String scope);

    /**
     * Verifies a JWT bearer token.
     *
     * @param tokenRepresentation A token representation including the token to verify.
     * @return Result of the validation.
     */
    Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation);

    /**
     * Verifies a JWT bearer token.
     *
     * @param token The token to verify.
     * @return Result of the validation.
     */
    default Result<ClaimToken> verifyJwtToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();

        return verifyJwtToken(tokenRepresentation);
    }
}
