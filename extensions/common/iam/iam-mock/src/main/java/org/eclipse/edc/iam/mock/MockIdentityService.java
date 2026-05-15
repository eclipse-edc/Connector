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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.edc.iam.mock;

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.iam.mock.IamMockExtension.DEFAULT_FAULTY_CLIENT_ID;
import static org.eclipse.edc.iam.mock.IamMockExtension.EDC_MOCK_FAULTY_CLIENT_ID;
import static org.eclipse.edc.iam.mock.IamMockExtension.PARTICIPANT_ID;

public class MockIdentityService implements IdentityService {
    private final ParticipantContextConfig contextConfig;
    private final TypeManager typeManager;

    public MockIdentityService(ParticipantContextConfig contextConfig, TypeManager typeManager) {
        this.contextConfig = contextConfig;
        this.typeManager = typeManager;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(String participantContextId, TokenParameters parameters) {

        var clientId = contextConfig.getString(participantContextId, PARTICIPANT_ID);
        var token = new MockToken();
        token.setClientId(clientId);
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(String participantContextId, TokenRepresentation tokenRepresentation, VerificationContext context) {
        var token = typeManager.readValue(tokenRepresentation.getToken(), MockToken.class);
        var faultyClientId = contextConfig.getString(participantContextId, EDC_MOCK_FAULTY_CLIENT_ID, DEFAULT_FAULTY_CLIENT_ID);

        if (faultyClientId.equals(token.clientId)) {
            return Result.failure("Unauthorized");
        }

        return Result.success(ClaimToken.Builder.newInstance()
                .claim("client_id", token.getClientId())
                .build());
    }

    private static class MockToken {
        private String audience;
        private String clientId;

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
