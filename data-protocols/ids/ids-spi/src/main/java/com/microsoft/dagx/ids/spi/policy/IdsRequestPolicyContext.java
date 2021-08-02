/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

import com.microsoft.dagx.spi.iam.ClaimToken;

/**
 * IDS policy context for provider-side request evaluation functions.
 */
public class IdsRequestPolicyContext {
    private String clientConnectorId;
    private final String correlationId;
    private ClaimToken claimToken;

    public String getClientConnectorId() {
        return clientConnectorId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }

    public IdsRequestPolicyContext(String clientConnectorId, String correlationId, ClaimToken claimToken) {
        this.clientConnectorId = clientConnectorId;
        this.correlationId = correlationId;
        this.claimToken = claimToken;
    }
}
