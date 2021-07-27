/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.iam;

/**
 * Obtains client security tokens from an identity provider.
 *
 * Providers may implement different authorization protocols such as OAuth2.
 */
public interface IdentityService {

    /**
     * Obtains a client token encoded as a JWT.
     */
    TokenResult obtainClientCredentials(String scope);

    /**
     * Verifies a JWT bearer token.
     *
     * @param token the token to verify
     * @param audience the audience the token must be for
     */
    VerificationResult verifyJwtToken(String token, String audience);

}
