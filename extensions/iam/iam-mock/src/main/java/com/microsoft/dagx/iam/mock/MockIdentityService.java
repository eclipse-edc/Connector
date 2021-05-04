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
        return TokenResult.Builder.newInstance().token("mock-eu").expiresIn(Instant.now().plusSeconds(10_0000).toEpochMilli()).build();
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        switch (token) {
            case "mock-eu":
                return new VerificationResult(ClaimToken.Builder.newInstance().claim("region", "eu").build());
            case "mock-us":
                return new VerificationResult(ClaimToken.Builder.newInstance().claim("region", "us").build());
            case "mock-an":
                return new VerificationResult(ClaimToken.Builder.newInstance().claim("region", "an").build());
        }
        return new VerificationResult("Unknown test token format");
    }
}
