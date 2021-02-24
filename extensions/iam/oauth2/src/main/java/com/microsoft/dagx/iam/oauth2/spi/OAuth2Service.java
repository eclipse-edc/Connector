package com.microsoft.dagx.iam.oauth2.spi;

/**
 * Implements the OAuth 2 client credentials flow and token verification.
 */
public interface OAuth2Service {

    /**
     * Obtains an bearer token using OAuth2 client credentials flow.
     */
    String obtainClientCredentials(String scope);

    /**
     * Verifies a JWT bearer token.
     *
     * @param token the token to verify
     */
    VerificationResult verifyJwtToken(String token);

}
