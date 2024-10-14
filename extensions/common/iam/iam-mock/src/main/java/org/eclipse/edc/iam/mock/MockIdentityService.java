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

package org.eclipse.edc.iam.mock;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

public class MockIdentityService implements IdentityService {
    private final String region;
    private final TypeManager typeManager;
    private final String clientId;
    private final String faultyClientId;

    public MockIdentityService(TypeManager typeManager, String region, String clientId, String faultyClientId) {
        this.typeManager = typeManager;
        this.region = region;
        this.clientId = clientId;
        this.faultyClientId = faultyClientId;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var token = new MockToken();
        token.setAudience(parameters.getStringClaim("aud"));
        token.setRegion(region);
        token.setClientId(clientId);
        TokenRepresentation tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext context) {
        var token = typeManager.readValue(tokenRepresentation.getToken(), MockToken.class);

        if (faultyClientId.equals(token.clientId)) {
            return Result.failure("Unauthorized");
        }

        return Result.success(ClaimToken.Builder.newInstance()
                .claim("region", token.region)
                .claim("client_id", token.clientId)
                .build());
    }

    private static class MockToken {
        private String region;
        private String audience;
        private String clientId;

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
