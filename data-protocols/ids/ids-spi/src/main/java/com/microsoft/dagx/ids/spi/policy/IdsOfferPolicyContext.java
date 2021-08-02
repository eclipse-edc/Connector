/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.policy;

/**
 * IDS policy context for client-side offer evaluation functions.
 */
public class IdsOfferPolicyContext {
    private String providerConnectorId;
    private final String processId;

    public String getProviderConnectorId() {
        return providerConnectorId;
    }

    public String getProcessId() {
        return processId;
    }

    public IdsOfferPolicyContext(String providerConnectorId, String processId) {
        this.providerConnectorId = providerConnectorId;
        this.processId = processId;
    }
}
