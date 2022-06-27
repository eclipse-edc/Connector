/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.dataspaceconnector.spi.iam;

import org.eclipse.dataspaceconnector.spi.result.Result;

/**
 * Obtains client security tokens from an identity provider.
 * Providers may implement different authorization protocols such as OAuth2.
 */
public interface IdentityService {

    /**
     * Obtains a client token encoded as a JWT.
     *
     * @param parameters parameter object defining the token properties.
     * @return generated client token.
     */
    Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters);

    /**
     * Verifies a JWT bearer token.
     *
     * @param tokenRepresentation A token representation including the token to verify.
     * @param audience Expected audience.
     * @return Result of the validation.
     */
    Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience);
}
