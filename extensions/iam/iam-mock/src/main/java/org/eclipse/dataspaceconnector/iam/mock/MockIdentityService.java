/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.mock;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;

import java.time.Instant;

/**
 *
 */
public class MockIdentityService implements IdentityService {
    private final String region;

    public MockIdentityService(String region) {
        this.region = region;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        return TokenResult.Builder.newInstance().token("mock-" + region).expiresIn(Instant.now().plusSeconds(10_0000).toEpochMilli()).build();
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
