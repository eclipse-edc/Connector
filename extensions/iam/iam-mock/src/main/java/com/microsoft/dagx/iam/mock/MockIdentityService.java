package com.microsoft.dagx.iam.mock;

import com.microsoft.dagx.spi.iam.ClaimToken;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.iam.TokenResult;
import com.microsoft.dagx.spi.iam.VerificationResult;

import java.time.Instant;

/**
 *
 */
public class MockIdentityService implements IdentityService {

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        return TokenResult.Builder.newInstance().token("mock-token").expiresIn(Instant.now().plusSeconds(10_0000).toEpochMilli()).build();
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        return token.equals("mock-token") ? new VerificationResult(ClaimToken.Builder.newInstance().claim("region", "eu").build()) : new VerificationResult("Unknown test token format");
    }
}
